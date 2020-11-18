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
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Insn;
import jlitec.backend.arch.arm.MemoryAddress;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Program;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.arch.arm.Size;
import jlitec.backend.arch.arm.insn.ADDInsn;
import jlitec.backend.arch.arm.insn.ANDInsn;
import jlitec.backend.arch.arm.insn.BInsn;
import jlitec.backend.arch.arm.insn.BLInsn;
import jlitec.backend.arch.arm.insn.BXInsn;
import jlitec.backend.arch.arm.insn.CMPInsn;
import jlitec.backend.arch.arm.insn.LDMFDInsn;
import jlitec.backend.arch.arm.insn.LDRInsn;
import jlitec.backend.arch.arm.insn.LabelInsn;
import jlitec.backend.arch.arm.insn.MOVInsn;
import jlitec.backend.arch.arm.insn.MULInsn;
import jlitec.backend.arch.arm.insn.MVNInsn;
import jlitec.backend.arch.arm.insn.ORRInsn;
import jlitec.backend.arch.arm.insn.RSBInsn;
import jlitec.backend.arch.arm.insn.SDIVInsn;
import jlitec.backend.arch.arm.insn.STMFDInsn;
import jlitec.backend.arch.arm.insn.STRInsn;
import jlitec.backend.arch.arm.insn.SUBInsn;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
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

  public static Program gen(jlitec.ir3.Program input) {
    final var program = new LowerPass().pass(input);
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

    // TODO add helper functions: getline_without_newline, readln_int_bool, println_int,
    // println_bool
    /* println_int:
     *    mov r1, r0
     *    ldr r0, .PERCENTD
     *    b printf
     * */
    insnList.add(new AssemblerDirective("global", "println_int"));
    insnList.add(new AssemblerDirective("type", "println_int, %function"));
    insnList.add(new LabelInsn("println_int"));
    insnList.add(new MOVInsn(Condition.AL, Register.R1, new Operand2.Register(Register.R0)));
    insnList.add(
        new LDRInsn(
            Condition.AL,
            Size.WORD,
            Register.R0,
            new MemoryAddress.PCRelative(stringGen.gen("%d\n"))));
    insnList.add(new BInsn(Condition.AL, "printf"));

    // Store generated strings.
    for (final var label : stringGen.getStringToId().values()) {
      insnList.add(new AssemblerDirective("align", "2"));
      insnList.add(new LabelInsn(label));
      insnList.add(new AssemblerDirective("word", label + "S"));
    }
    for (final var entry : stringGen.getStringToId().entrySet()) {
      final var string = entry.getKey();
      final var label = entry.getValue();
      insnList.add(new AssemblerDirective("align", "2"));
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
      Set<Register> stmfdRegs, Set<Register> ldmfdRegs, Map<String, Integer> offsets) {
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
      stmfdRegs = Set.of(Register.R4, Register.LR);
      ldmfdRegs = Set.of(Register.R4, Register.PC);
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
    final var stackArgOffset = (method.spilled().size() + stmfdRegs.size()) * 4;
    for (int i = 4; i < method.argsWithThis().size(); i++) {
      final var arg = method.argsWithThis().get(i);
      offsets.put(arg.id(), stackArgOffset + (i - 4) * 4);
    }
    return new StackDescriptor(stmfdRegs, ldmfdRegs, offsets);
  }

  private static List<Insn> gen(Method method, StringGen stringGen, Map<String, Data> dataMap) {
    final var typeMap =
        Stream.concat(method.vars().stream(), method.argsWithThis().stream())
            .collect(Collectors.toUnmodifiableMap(Var::id, Var::type));
    final var regAllocOutput = new RegAllocPass().pass(method);
    final var regAllocMap = regAllocOutput.color();
    final var stackDesc = calculateStackDescriptor(regAllocOutput);

    final var result = new ArrayList<Insn>();

    result.add(new AssemblerDirective("global", method.id()));
    result.add(new AssemblerDirective("type", method.id() + ", %function"));
    result.add(new LabelInsn(method.id()));
    if (!stackDesc.stmfdRegs.isEmpty()) {
      result.add(new STMFDInsn(Register.SP, EnumSet.copyOf(stackDesc.stmfdRegs), true));
    }

    if (!regAllocOutput.method().spilled().isEmpty()) {
      result.add(
          new SUBInsn(
              Condition.AL,
              false,
              Register.SP,
              Register.SP,
              new Operand2.Immediate(regAllocOutput.method().spilled().size() * 4)));
    }

    for (final var stmt : regAllocOutput.method().lowerStmtList()) {
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
            case BINARY -> {
              final var bs = (BinaryLowerStmt) stmt;
              final var lhs = toRegister(bs.lhs(), regAllocMap);
              final var rhs = toRegister(bs.rhs(), regAllocMap);
              final var dest = toRegister(bs.dest(), regAllocMap);
              yield switch (bs.op()) {
                case LT -> List.of(
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new CMPInsn(Condition.AL, lhs, new Operand2.Register(rhs)),
                    new MOVInsn(Condition.LT, dest, new Operand2.Immediate(1)));
                case GT -> List.of(
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new CMPInsn(Condition.AL, lhs, new Operand2.Register(rhs)),
                    new MOVInsn(Condition.GT, dest, new Operand2.Immediate(1)));
                case LEQ -> List.of(
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new CMPInsn(Condition.AL, lhs, new Operand2.Register(rhs)),
                    new MOVInsn(Condition.LE, dest, new Operand2.Immediate(1)));
                case GEQ -> List.of(
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new CMPInsn(Condition.AL, lhs, new Operand2.Register(rhs)),
                    new MOVInsn(Condition.GE, dest, new Operand2.Immediate(1)));
                case EQ -> List.of(
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new CMPInsn(Condition.AL, lhs, new Operand2.Register(rhs)),
                    new MOVInsn(Condition.EQ, dest, new Operand2.Immediate(1)));
                case NEQ -> List.of(
                    new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                    new CMPInsn(Condition.AL, lhs, new Operand2.Register(rhs)),
                    new MOVInsn(Condition.NE, dest, new Operand2.Immediate(1)));
                case OR -> List.of(
                    new ORRInsn(Condition.AL, false, dest, lhs, new Operand2.Register(rhs)));
                case AND -> List.of(
                    new ANDInsn(Condition.AL, false, dest, lhs, new Operand2.Register(rhs)));
                case MULT -> List.of(new MULInsn(Condition.AL, false, dest, lhs, rhs));
                case DIV -> List.of(new SDIVInsn(Condition.AL, dest, lhs, rhs));
                case PLUS -> List.of(
                    new ADDInsn(Condition.AL, false, dest, lhs, new Operand2.Register(rhs)));
                case MINUS -> List.of(
                    new SUBInsn(Condition.AL, false, dest, lhs, new Operand2.Register(rhs)));
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
              if (stackDesc.ldmfdRegs.isEmpty()) {
                yield List.of(new BXInsn(Condition.AL, Register.LR));
              }
              yield List.of(new LDMFDInsn(Register.SP, EnumSet.copyOf(stackDesc.ldmfdRegs), true));
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
          };
      result.addAll(stmtChunk);
    }

    return Collections.unmodifiableList(result);
  }
}
