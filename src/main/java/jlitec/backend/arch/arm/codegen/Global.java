package jlitec.backend.arch.arm.codegen;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.AssemblerDirective;
import jlitec.backend.arch.arm.BitInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Insn;
import jlitec.backend.arch.arm.MemoryAddress;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Program;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.arch.arm.Size;
import jlitec.backend.arch.arm.insn.ADDInsn;
import jlitec.backend.arch.arm.insn.ANDInsn;
import jlitec.backend.arch.arm.insn.ASRInsn;
import jlitec.backend.arch.arm.insn.BInsn;
import jlitec.backend.arch.arm.insn.BLInsn;
import jlitec.backend.arch.arm.insn.BXInsn;
import jlitec.backend.arch.arm.insn.CMPInsn;
import jlitec.backend.arch.arm.insn.LDMFDInsn;
import jlitec.backend.arch.arm.insn.LDRInsn;
import jlitec.backend.arch.arm.insn.LSLInsn;
import jlitec.backend.arch.arm.insn.LSRInsn;
import jlitec.backend.arch.arm.insn.LabelInsn;
import jlitec.backend.arch.arm.insn.MOVInsn;
import jlitec.backend.arch.arm.insn.MULInsn;
import jlitec.backend.arch.arm.insn.MVNInsn;
import jlitec.backend.arch.arm.insn.ORRInsn;
import jlitec.backend.arch.arm.insn.RORInsn;
import jlitec.backend.arch.arm.insn.RSBInsn;
import jlitec.backend.arch.arm.insn.SDIVInsn;
import jlitec.backend.arch.arm.insn.STMFDInsn;
import jlitec.backend.arch.arm.insn.STRInsn;
import jlitec.backend.arch.arm.insn.SUBInsn;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.BranchLinkLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAssignLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LabelLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadStackArgLowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.PopStackLowerStmt;
import jlitec.backend.passes.lower.stmt.PushStackLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractLowerStmt;
import jlitec.backend.passes.lower.stmt.StoreSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;
import jlitec.backend.passes.regalloc.RegAllocPass;
import jlitec.ir3.Data;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;
import org.apache.commons.text.StringEscapeUtils;

public class Global {
  // Prevent instantiation
  private Global() {}

  private static final Set<String> helperFunctions =
      Set.of("readln_int_bool", "println_bool", "getline_without_newline");

  public static Program gen(jlitec.backend.passes.lower.Program program) {
    final var insnList = new ArrayList<Insn>();

    final var stringGen = new StringGen();
    final var dataMap =
        program.dataList().stream()
            .collect(Collectors.toUnmodifiableMap(Data::cname, Function.identity()));

    insnList.add(new AssemblerDirective("cpu", "cortex-a7"));

    final var methodInsnList =
        program.methodList().stream()
            .flatMap(method -> gen(method, stringGen, dataMap).stream())
            .collect(Collectors.toUnmodifiableList());
    insnList.addAll(methodInsnList);

    final var helperFunctionsCalled =
        methodInsnList.stream()
            .flatMap(i -> i instanceof BLInsn bli ? Stream.of(bli.label()) : Stream.empty())
            .filter(helperFunctions::contains)
            .collect(Collectors.toUnmodifiableSet());
    if (helperFunctionsCalled.contains("readln_int_bool")) {
      addReadlnIntBool(insnList, stringGen);
    }
    if (helperFunctionsCalled.contains("println_bool")) {
      addPrintlnBool(insnList, stringGen);
    }
    if (helperFunctionsCalled.contains("getline_without_newline")) {
      addGetlineWithoutNewline(insnList);
      // stdin stream
      insnList.add(new LabelInsn(".Lstdin"));
      insnList.add(new AssemblerDirective("word", "stdin"));
    }

    // Store generated strings.
    for (final var label : stringGen.getStringToId().values()) {
      insnList.add(new LabelInsn(label));
      insnList.add(new AssemblerDirective("word", label + "S"));
    }
    for (final var entry : stringGen.getStringToId().entrySet()) {
      final var string = entry.getKey();
      final var label = entry.getValue();
      insnList.add(new AssemblerDirective("section", ".rodata"));
      insnList.add(new LabelInsn(label + "S"));
      insnList.add(
          new AssemblerDirective("asciz", '"' + StringEscapeUtils.escapeJava(string) + '"'));
    }

    return new Program(insnList);
  }

  private static Register toRegister(Addressable a, Map<String, Register> regAllocMap) {
    return switch (a.type()) {
      case REG -> ((Addressable.Reg) a).reg();
      case ID_RVAL -> regAllocMap.get(((Addressable.IdRval) a).idRvalExpr().id());
    };
  }

  private static record StackDescriptor(
      Set<Register> stmfdRegs,
      Set<Register> ldmfdRegs,
      Map<String, Integer> offsets,
      int totalOffset) {
    public StackDescriptor {
      if ((stmfdRegs.size() & 1) == 1) {
        throw new RuntimeException(
            "stack must be 8-bytes aligned, instead stmfdRegs size is " + stmfdRegs.size());
      }
      if ((ldmfdRegs.size() & 1) == 1) {
        throw new RuntimeException(
            "stack must be 8-bytes aligned, instead ldmfdRegs size is " + ldmfdRegs.size());
      }
      if (stmfdRegs.size() != ldmfdRegs.size()) {
        throw new RuntimeException(
            "stfmdRegs size is not equal to ldmfdRegs. They should be equal.");
      }
      if (!stmfdRegs.isEmpty()
          && !(stmfdRegs.contains(Register.LR) && !stmfdRegs.contains(Register.PC))) {
        throw new RuntimeException(
            "STMFD should be either (empty) or (contain LR and not contain PC), instead = "
                + stmfdRegs);
      }
      if (!ldmfdRegs.isEmpty()
          && !(ldmfdRegs.contains(Register.PC) && !ldmfdRegs.contains(Register.LR))) {
        throw new RuntimeException(
            "LDMFD should be either (empty) or (contain PC and not contain LR), instead = "
                + ldmfdRegs);
      }
      this.stmfdRegs = Collections.unmodifiableSet(stmfdRegs);
      this.ldmfdRegs = Collections.unmodifiableSet(ldmfdRegs);
      this.offsets = Collections.unmodifiableMap(offsets);
    }
  }

  private static StackDescriptor calculateStackDescriptor(RegAllocPass.Output regAllocOutput) {
    final var hasBL =
        regAllocOutput.method().lowerStmtList().stream()
            .anyMatch(s -> s instanceof BranchLinkLowerStmt);
    final Set<Register> ldmfdRegs, stmfdRegs;
    final var maxRegNum =
        regAllocOutput.color().values().stream().mapToInt(Register::toInt).max().orElseGet(() -> 3);
    if (maxRegNum >= 4) {
      // Involving callee-save registers
      // Push r3 too if number of registers are even (so it will be even with the addition of LR)
      final var pushedRegisters =
          IntStream.range((maxRegNum & 1) == 1 ? 3 : 4, maxRegNum + 1)
              .boxed()
              .map(Register::fromInt)
              .collect(Collectors.toUnmodifiableSet());
      stmfdRegs = ImmutableSet.<Register>builder().add(Register.LR).addAll(pushedRegisters).build();
      ldmfdRegs = ImmutableSet.<Register>builder().add(Register.PC).addAll(pushedRegisters).build();
    } else if (hasBL) {
      stmfdRegs = EnumSet.of(Register.R4, Register.LR);
      ldmfdRegs = EnumSet.of(Register.R4, Register.PC);
    } else {
      // No callee-save registers and no BL
      stmfdRegs = Set.of();
      ldmfdRegs = Set.of();
    }
    final var method = regAllocOutput.method();
    final var offsets = new HashMap<String, Integer>();
    for (int i = 0; i < method.spilled().size(); i++) {
      final var variable = method.spilled().get(i);
      offsets.put(variable.id(), i * 4);
    }
    final var totalOffsetOriginal = method.spilled().size() * 4;
    final var totalOffsetAligned =
        totalOffsetOriginal % 8 == 0 ? totalOffsetOriginal : totalOffsetOriginal + 4;
    final var stackArgOffset = totalOffsetAligned + stmfdRegs.size() * 4;
    for (int i = 4; i < method.argsWithThis().size(); i++) {
      final var arg = method.argsWithThis().get(i);
      offsets.put(arg.id(), stackArgOffset + (i - 4) * 4);
    }
    return new StackDescriptor(stmfdRegs, ldmfdRegs, offsets, totalOffsetAligned);
  }

  private static List<Insn> gen(Method input, StringGen stringGen, Map<String, Data> dataMap) {
    final var regAllocOutput = new RegAllocPass().pass(input);
    final var method = regAllocOutput.method();
    final var regAllocMap = regAllocOutput.color();
    final var stackDesc = calculateStackDescriptor(regAllocOutput);

    final var typeMap =
        Stream.concat(
                method.vars().stream(),
                Stream.concat(method.argsWithThis().stream(), method.spilled().stream()))
            .distinct()
            .collect(Collectors.toUnmodifiableMap(Var::id, Var::type));

    final var result = new ArrayList<Insn>();

    addFunctionPreamble(result, method.id());

    if (!stackDesc.stmfdRegs.isEmpty()) {
      result.add(new STMFDInsn(Register.SP, EnumSet.copyOf(stackDesc.stmfdRegs), true));
    }

    if (stackDesc.totalOffset != 0) {
      result.add(
          new SUBInsn(
              Condition.AL,
              false,
              Register.SP,
              Register.SP,
              new Operand2.Immediate(stackDesc.totalOffset)));
    }

    for (final var stmt : method.lowerStmtList()) {
      final List<Insn> stmtChunk =
          switch (stmt.stmtExtensionType()) {
            case LOAD_STACK_ARG -> {
              final var lsas = (LoadStackArgLowerStmt) stmt;
              final var dest = regAllocMap.get(lsas.stackArg().id());
              final var offset = stackDesc.offsets.get(lsas.stackArg().id());
              yield List.of(
                  new LDRInsn(
                      Condition.AL,
                      Size.WORD,
                      dest,
                      new MemoryAddress.ImmediateOffset(Register.SP, offset)));
            }
            case REVERSE_SUBTRACT -> {
              final var bs = (ReverseSubtractLowerStmt) stmt;
              final var lhs = toRegister(bs.lhs(), regAllocMap);
              final var rhs =
                  switch (bs.rhs().getRvalExprType()) {
                    case ID -> {
                      final var ire = (IdRvalExpr) bs.rhs();
                      yield new Operand2.Register(regAllocMap.get(ire.id()));
                    }
                    case INT -> {
                      final var ire = (IntRvalExpr) bs.rhs();
                      yield new Operand2.Immediate(ire.value());
                    }
                    case STRING, NULL, BOOL -> throw new RuntimeException("should not be reached");
                  };
              final var dest = toRegister(bs.dest(), regAllocMap);
              yield List.of(new RSBInsn(Condition.AL, false, dest, lhs, rhs));
            }
            case BINARY -> {
              final var bs = (BinaryLowerStmt) stmt;
              final var lhs = toRegister(bs.lhs(), regAllocMap);
              final var rhs =
                  switch (bs.rhs().getRvalExprType()) {
                    case ID -> {
                      final var ire = (IdRvalExpr) bs.rhs();
                      yield new Operand2.Register(regAllocMap.get(ire.id()));
                    }
                    case INT -> {
                      final var ire = (IntRvalExpr) bs.rhs();
                      yield new Operand2.Immediate(ire.value());
                    }
                    case BOOL -> {
                      final var bre = (BoolRvalExpr) bs.rhs();
                      yield new Operand2.Immediate(bre.value() ? 1 : 0);
                    }
                    case STRING, NULL -> throw new RuntimeException("should not be reached");
                  };
              final var dest = toRegister(bs.dest(), regAllocMap);
              yield switch (bs.op()) {
                case LT -> List.of(
                    new CMPInsn(Condition.AL, lhs, rhs),
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new MOVInsn(Condition.LT, dest, new Operand2.Immediate(1)));
                case GT -> List.of(
                    new CMPInsn(Condition.AL, lhs, rhs),
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new MOVInsn(Condition.GT, dest, new Operand2.Immediate(1)));
                case LEQ -> List.of(
                    new CMPInsn(Condition.AL, lhs, rhs),
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new MOVInsn(Condition.LE, dest, new Operand2.Immediate(1)));
                case GEQ -> List.of(
                    new CMPInsn(Condition.AL, lhs, rhs),
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new MOVInsn(Condition.GE, dest, new Operand2.Immediate(1)));
                case EQ -> List.of(
                    new CMPInsn(Condition.AL, lhs, rhs),
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new MOVInsn(Condition.EQ, dest, new Operand2.Immediate(1)));
                case NEQ -> List.of(
                    new SUBInsn(Condition.AL, true, dest, lhs, rhs),
                    new MOVInsn(Condition.NE, dest, new Operand2.Immediate(1)));
                case OR -> List.of(new ORRInsn(Condition.AL, false, dest, lhs, rhs));
                case AND -> List.of(new ANDInsn(Condition.AL, false, dest, lhs, rhs));
                case PLUS -> List.of(new ADDInsn(Condition.AL, false, dest, lhs, rhs));
                case MINUS -> List.of(new SUBInsn(Condition.AL, false, dest, lhs, rhs));
                case MULT, DIV -> throw new RuntimeException("should not be reached");
              };
            }
            case REG_BINARY -> {
              final var bs = (RegBinaryLowerStmt) stmt;
              final var lhs = toRegister(bs.lhs(), regAllocMap);
              final var rhs = toRegister(bs.rhs(), regAllocMap);
              final var dest = toRegister(bs.dest(), regAllocMap);
              yield switch (bs.op()) {
                case LT, GT, LEQ, GEQ, EQ, NEQ, OR, AND, PLUS, MINUS -> throw new RuntimeException(
                    "should not be reached");
                case MULT -> List.of(new MULInsn(Condition.AL, false, dest, lhs, rhs));
                case DIV -> List.of(new SDIVInsn(Condition.AL, dest, lhs, rhs));
              };
            }
            case BRANCH_LINK -> {
              final var bls = (BranchLinkLowerStmt) stmt;
              yield List.of(new BLInsn(Condition.AL, bls.target()));
            }
            case CMP -> {
              final var cs = (CmpLowerStmt) stmt;
              final var cond =
                  switch (cs.op()) {
                    case EQ -> Condition.EQ;
                    case LT -> Condition.LT;
                    case GT -> Condition.GT;
                    case AND, OR, MULT, DIV, PLUS, MINUS -> throw new RuntimeException(
                        "IR3 if statements contains op not in RelExp");
                    case LEQ -> Condition.LE;
                    case GEQ -> Condition.GE;
                    case NEQ -> Condition.NE;
                  };
              final var lhs = regAllocMap.get(cs.lhs().id());
              final Operand2 rhs =
                  switch (cs.rhs().getRvalExprType()) {
                    case ID -> {
                      final var ie = (IdRvalExpr) cs.rhs();
                      final var reg = regAllocMap.get(ie.id());
                      yield new Operand2.Register(reg);
                    }
                    case BOOL -> {
                      final var be = (BoolRvalExpr) cs.rhs();
                      yield new Operand2.Immediate(be.value() ? 1 : 0);
                    }
                    case INT -> {
                      final var be = (IntRvalExpr) cs.rhs();
                      // TODO handle large immediate
                      yield new Operand2.Immediate(be.value());
                    }
                    case STRING, NULL -> throw new RuntimeException("invalid type in CMP");
                  };
              yield List.of(new CMPInsn(Condition.AL, lhs, rhs), new BInsn(cond, cs.dest()));
            }
            case FIELD_ASSIGN -> {
              final var fas = (FieldAssignLowerStmt) stmt;
              final var rhs = toRegister(fas.rhs(), regAllocMap);
              final var lhsIdReg = regAllocMap.get(fas.lhsId().id());
              final var cname = ((Type.KlassType) typeMap.get(fas.lhsId().id())).cname();
              final var data = dataMap.get(cname);
              final var fieldIndex =
                  IntStream.range(0, data.fields().size())
                      .filter(i -> data.fields().get(i).id().equals(fas.lhsField()))
                      .findFirst()
                      .getAsInt();
              final var fieldOffset = fieldIndex * 4;
              yield List.of(
                  new STRInsn(
                      Condition.AL,
                      Size.WORD,
                      rhs,
                      new MemoryAddress.ImmediateOffset(lhsIdReg, fieldOffset)));
            }
            case FIELD_ACCESS -> {
              final var fas = (FieldAccessLowerStmt) stmt;
              final var rhsIdReg = regAllocMap.get(fas.rhsId().id());
              final var lhsReg = regAllocMap.get(fas.lhs().id());
              final var cname = ((Type.KlassType) typeMap.get(fas.rhsId().id())).cname();
              final var data = dataMap.get(cname);
              final var fieldIndex =
                  IntStream.range(0, data.fields().size())
                      .filter(i -> data.fields().get(i).id().equals(fas.rhsField()))
                      .findFirst()
                      .getAsInt();
              final var fieldOffset = fieldIndex * 4;
              yield List.of(
                  new LDRInsn(
                      Condition.AL,
                      Size.WORD,
                      lhsReg,
                      new MemoryAddress.ImmediateOffset(rhsIdReg, fieldOffset)));
            }
            case GOTO -> {
              final var gs = (GotoLowerStmt) stmt;
              yield List.of(new BInsn(Condition.AL, gs.dest()));
            }
            case IMMEDIATE -> {
              final var is = (ImmediateLowerStmt) stmt;
              final var dest = toRegister(is.dest(), regAllocMap);
              yield switch (is.value().getRvalExprType()) {
                case ID -> throw new RuntimeException("Impossible");
                case BOOL -> {
                  final var be = (BoolRvalExpr) is.value();
                  yield List.of(
                      new MOVInsn(Condition.AL, dest, new Operand2.Immediate(be.value() ? 1 : 0)));
                }
                case INT -> {
                  final var ie = (IntRvalExpr) is.value();
                  // TODO handle large immediate
                  yield List.of(
                      new MOVInsn(Condition.AL, dest, new Operand2.Immediate(ie.value())));
                }
                case STRING -> {
                  final var se = (StringRvalExpr) is.value();
                  final var label = stringGen.gen(se.value());
                  yield List.of(
                      new LDRInsn(
                          Condition.AL, Size.WORD, dest, new MemoryAddress.PCRelative(label)));
                }
                case NULL -> {
                  final var label = stringGen.gen("");
                  yield List.of(
                      new LDRInsn(
                          Condition.AL, Size.WORD, dest, new MemoryAddress.PCRelative(label)));
                }
              };
            }
            case LABEL -> {
              final var ls = (LabelLowerStmt) stmt;
              yield List.of(new LabelInsn(ls.label()));
            }
            case LDR_SPILL -> {
              final var lss = (LoadSpilledLowerStmt) stmt;
              final var dstReg = regAllocMap.get(lss.dst().id());
              final var offset = stackDesc.offsets.get(lss.varName());
              yield List.of(
                  new LDRInsn(
                      Condition.AL,
                      Size.WORD,
                      dstReg,
                      new MemoryAddress.ImmediateOffset(Register.SP, offset)));
            }
            case STR_SPILL -> {
              final var sss = (StoreSpilledLowerStmt) stmt;
              final var srcReg = regAllocMap.get(sss.src().id());
              final var offset = stackDesc.offsets.get(sss.varName());
              yield List.of(
                  new STRInsn(
                      Condition.AL,
                      Size.WORD,
                      srcReg,
                      new MemoryAddress.ImmediateOffset(Register.SP, offset)));
            }
            case RETURN -> {
              final var insnChunk = new ArrayList<Insn>();
              if (method.id().equals("main")) {
                insnChunk.add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Immediate(0)));
              }
              if (stackDesc.totalOffset != 0) {
                insnChunk.add(
                    new ADDInsn(
                        Condition.AL,
                        false,
                        Register.SP,
                        Register.SP,
                        new Operand2.Immediate(stackDesc.totalOffset)));
              }
              if (stackDesc.ldmfdRegs.isEmpty()) {
                insnChunk.add(new BXInsn(Condition.AL, Register.LR));
              } else {
                insnChunk.add(
                    new LDMFDInsn(Register.SP, EnumSet.copyOf(stackDesc.ldmfdRegs), true));
              }
              yield Collections.unmodifiableList(insnChunk);
            }
            case MOV -> {
              final var ms = (MovLowerStmt) stmt;
              final var dst = toRegister(ms.dst(), regAllocMap);
              final var src = toRegister(ms.src(), regAllocMap);
              if (dst.equals(src)) {
                yield List.of();
              }
              yield List.of(new MOVInsn(Condition.AL, dst, new Operand2.Register(src)));
            }
            case PUSH_PAD_STACK -> List.of(
                new SUBInsn(
                    Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(4)));
            case PUSH_STACK -> {
              final var pss = (PushStackLowerStmt) stmt;
              final var src = regAllocMap.get(pss.idRvalExpr().id());
              yield List.of(
                  new STRInsn(
                      Condition.AL,
                      Size.WORD,
                      src,
                      new MemoryAddress.ImmediateOffset(Register.SP, -4, true)));
            }
            case POP_STACK -> {
              final var pss = (PopStackLowerStmt) stmt;
              final var numBytes = pss.num() * 4;
              // TODO handle large immediate
              yield List.of(
                  new ADDInsn(
                      Condition.AL,
                      false,
                      Register.SP,
                      Register.SP,
                      new Operand2.Immediate(numBytes)));
            }
            case UNARY -> {
              final var ue = (UnaryLowerStmt) stmt;
              final var dest = regAllocMap.get(ue.dest().id());
              final var expr = regAllocMap.get(ue.expr().id());
              yield switch (ue.op()) {
                case NOT -> List.of(new MVNInsn(Condition.AL, dest, new Operand2.Register(expr)));
                case NEGATIVE -> List.of(
                    new RSBInsn(Condition.AL, false, dest, expr, new Operand2.Immediate(0)));
              };
            }
            case BIT -> {
              final var bs = (BitLowerStmt) stmt;
              final var dest = regAllocMap.get(bs.dest().id());
              final var expr = regAllocMap.get(bs.expr().id());
              yield switch (bs.op()) {
                case LSL -> List.of(
                    new LSLInsn(Condition.AL, dest, expr, new BitInsn.Rssh.Immediate(bs.shift())));
                case ROR -> List.of(
                    new RORInsn(Condition.AL, dest, expr, new BitInsn.Rssh.Immediate(bs.shift())));
                case LSR -> List.of(
                    new LSRInsn(Condition.AL, dest, expr, new BitInsn.Rssh.Immediate(bs.shift())));
                case ASR -> List.of(
                    new ASRInsn(Condition.AL, dest, expr, new BitInsn.Rssh.Immediate(bs.shift())));
              };
            }
          };
      result.addAll(stmtChunk);
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * In ASM: <code>
   * readln_int_bool:
   *   stmfd sp!, {r4, lr}
   *   sub sp, sp, #16
   *   mov r0, #0
   *   str r0, [sp, #8]
   *   str r0, [sp, #4]
   *   ldr r0, .Lstdin
   *   ldr r2, [r0]
   *   add r0, sp, #8
   *   add r1, sp, #4
   *   bl getline
   *   ldr r0, [sp, #8]
   *   ldr r1, .PERCENTD
   *   add r2, sp, #12
   *   bl sscanf
   *   ldr r0, [sp, #8]
   *   bl free
   *   ldr r0, [sp, #12]
   *   add sp, sp, #16
   *   ldmfd sp!, {r4, pc}
   * </code> In C: <code>
   * int readln_int_bool()
   * {
   *   int a;
   *   char* result = NULL;
   *   size_t n = 0;
   *   getline(&result, &n, stdin);
   *   sscanf(result, "%d", &a);
   *   free(result);
   *   return a;
   * }
   * </code>
   *
   * @param insnList instruction list to add instructions to.
   */
  private static void addReadlnIntBool(List<Insn> insnList, StringGen stringGen) {
    addFunctionPreamble(insnList, "readln_int_bool");
    insnList.add(new STMFDInsn(Register.SP, EnumSet.of(Register.R4, Register.LR), true));
    insnList.add(
        new SUBInsn(Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(16)));
    insnList.add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Immediate(0)));
    insnList.add(
        new STRInsn(
            Condition.AL,
            Size.WORD,
            Register.R0,
            new MemoryAddress.ImmediateOffset(Register.SP, 8)));
    insnList.add(
        new STRInsn(
            Condition.AL,
            Size.WORD,
            Register.R0,
            new MemoryAddress.ImmediateOffset(Register.SP, 4)));
    insnList.add(
        new LDRInsn(Condition.AL, Size.WORD, Register.R0, new MemoryAddress.PCRelative(".Lstdin")));
    insnList.add(
        new LDRInsn(
            Condition.AL, Size.WORD, Register.R2, new MemoryAddress.ImmediateOffset(Register.R0)));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.R0, Register.SP, new Operand2.Immediate(8)));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.R1, Register.SP, new Operand2.Immediate(4)));
    insnList.add(new BLInsn(Condition.AL, "getline"));
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R0,
            new MemoryAddress.ImmediateOffset(Register.SP, 8)));
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R1,
            new MemoryAddress.PCRelative(stringGen.gen("%d"))));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.R2, Register.SP, new Operand2.Immediate(12)));
    insnList.add(new BLInsn(Condition.AL, "sscanf"));
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R0,
            new MemoryAddress.ImmediateOffset(Register.SP, 8)));
    insnList.add(new BLInsn(Condition.AL, "free"));
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R0,
            new MemoryAddress.ImmediateOffset(Register.SP, 12)));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(16)));
    insnList.add(new LDMFDInsn(Register.SP, EnumSet.of(Register.R4, Register.PC), true));
  }

  /**
   * In ASM: <code>
   * println_bool:
   *   ldr r2, .TRUE
   *   ldr r1, .FALSE
   *   cmp r0, #1
   *   moveq r1, r2
   *   mov r0, r1
   *   b puts
   * </code> In C: <code>
   * void println_bool(int a)
   * {
   *   puts(a == 1 ? "true" : "false");
   * }
   * </code>
   *
   * @param insnList instruction list to add instructions to.
   */
  private static void addPrintlnBool(List<Insn> insnList, StringGen stringGen) {
    addFunctionPreamble(insnList, "println_bool");
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R2,
            new MemoryAddress.PCRelative(stringGen.gen("true"))));
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R1,
            new MemoryAddress.PCRelative(stringGen.gen("false"))));
    insnList.add(new CMPInsn(Condition.AL, Register.R0, new Operand2.Immediate(1)));
    insnList.add(new MOVInsn(Condition.EQ, Register.R1, new Operand2.Register(Register.R2)));
    insnList.add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Register(Register.R1)));
    insnList.add(new BInsn(Condition.AL, "puts"));
  }

  /**
   * In ASM: <code>
   * getline_without_newline:
   *   STMFD SP!, {R4, LR}
   *   SUB SP, SP, #8
   *   MOV R4, #0
   *   STR R4, [SP]
   *   STR R4, [SP, #4]
   *   MOV R0, SP
   *   ADD R1, R0, #4
   *   LDR R3, .Lstdin
   *   LDR R2, [R3]
   *   BL getline
   *   SUB R1, R0, #1
   *   LDR R0, [SP]
   *   STRB R4, [R0, R1]
   *   BL realloc
   *   ADD SP, SP, #8
   *   LDMFD SP!, {R4, PC}
   * </code> In C: <code>
   * char* getline_without_newline() {
   *   char* result = NULL;
   *   size_t n = 0;
   *   ssize_t len = getline(&result, &n, stdin);
   *   result[len - 1] = 0;
   *   return realloc(result, len - 1);
   * }
   * </code>
   *
   * @param insnList
   */
  private static void addGetlineWithoutNewline(List<Insn> insnList) {
    addFunctionPreamble(insnList, "getline_without_newline");
    insnList.add(new STMFDInsn(Register.SP, EnumSet.of(Register.R4, Register.LR), true));
    insnList.add(
        new SUBInsn(Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(8)));
    insnList.add(new MOVInsn(Condition.AL, Register.R4, new Operand2.Immediate(0)));
    insnList.add(
        new STRInsn(
            Condition.AL, Size.WORD, Register.R4, new MemoryAddress.ImmediateOffset(Register.SP)));
    insnList.add(
        new STRInsn(
            Condition.AL,
            Size.WORD,
            Register.R4,
            new MemoryAddress.ImmediateOffset(Register.SP, 4)));
    insnList.add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Register(Register.SP)));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.R1, Register.R0, new Operand2.Immediate(4)));
    insnList.add(
        new LDRInsn(Condition.AL, Size.WORD, Register.R3, new MemoryAddress.PCRelative(".Lstdin")));
    insnList.add(
        new LDRInsn(
            Condition.AL, Size.WORD, Register.R2, new MemoryAddress.ImmediateOffset(Register.R3)));
    insnList.add(new BLInsn(Condition.AL, "getline"));
    insnList.add(
        new SUBInsn(Condition.AL, false, Register.R1, Register.R0, new Operand2.Immediate(1)));
    insnList.add(
        new LDRInsn(
            Condition.AL, Size.WORD, Register.R0, new MemoryAddress.ImmediateOffset(Register.SP)));
    insnList.add(
        new STRInsn(
            Condition.AL,
            Size.B,
            Register.R4,
            new MemoryAddress.RegisterOffset(Register.R0, Register.R1)));
    insnList.add(new BLInsn(Condition.AL, "realloc"));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(8)));
    insnList.add(new LDMFDInsn(Register.SP, EnumSet.of(Register.R4, Register.PC), true));
  }

  private static void addFunctionPreamble(List<Insn> insnList, String methodName) {
    insnList.add(new AssemblerDirective("global", methodName));
    insnList.add(new AssemblerDirective("type", methodName + ", %function"));
    insnList.add(new LabelInsn(methodName));
  }
}
