package jlitec.backend.arm.codegen;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.AssemblerDirective;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Insn;
import jlitec.backend.arm.MemoryAddress;
import jlitec.backend.arm.Operand2;
import jlitec.backend.arm.Program;
import jlitec.backend.arm.Register;
import jlitec.backend.arm.Size;
import jlitec.backend.arm.insn.ADDInsn;
import jlitec.backend.arm.insn.ANDInsn;
import jlitec.backend.arm.insn.BInsn;
import jlitec.backend.arm.insn.BLInsn;
import jlitec.backend.arm.insn.CMPInsn;
import jlitec.backend.arm.insn.LDMFDInsn;
import jlitec.backend.arm.insn.LDRInsn;
import jlitec.backend.arm.insn.LabelInsn;
import jlitec.backend.arm.insn.MOVInsn;
import jlitec.backend.arm.insn.MULInsn;
import jlitec.backend.arm.insn.MVNInsn;
import jlitec.backend.arm.insn.ORRInsn;
import jlitec.backend.arm.insn.RSBInsn;
import jlitec.backend.arm.insn.SDIVInsn;
import jlitec.backend.arm.insn.STMFDInsn;
import jlitec.backend.arm.insn.STRInsn;
import jlitec.backend.arm.insn.SUBInsn;
import jlitec.ir3.Method;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.expr.BinaryExpr;
import jlitec.ir3.expr.Expr;
import jlitec.ir3.expr.UnaryExpr;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;
import jlitec.ir3.stmt.CmpStmt;
import jlitec.ir3.stmt.GotoStmt;
import jlitec.ir3.stmt.LabelStmt;
import jlitec.ir3.stmt.PrintlnStmt;
import jlitec.ir3.stmt.ReturnStmt;
import jlitec.ir3.stmt.Stmt;
import jlitec.ir3.stmt.VarAssignStmt;
import org.apache.commons.text.StringEscapeUtils;

public class Simple {
  // Prevent instantiation
  private Simple() {}

  public static Program gen(jlitec.ir3.Program program) {
    final var insnList = new ArrayList<Insn>();
    final var stringGen = new StringGen();
    for (final var method : program.methodList()) {
      final var typeMap =
          Stream.concat(method.args().stream(), method.vars().stream())
              .collect(Collectors.toUnmodifiableMap(Var::id, Var::type));
      final var stackDesc = buildStackDesc(method);

      insnList.add(new AssemblerDirective("global", method.id().equals("main") ? "main" : "func"));
      insnList.add(new AssemblerDirective("type", method.id() + ", %function"));
      insnList.add(new LabelInsn(method.id()));

      insnList.add(
          new STMFDInsn(
              Register.SP, EnumSet.of(Register.R4, Register.R5, Register.R6, Register.LR), true));

      if (stackDesc.totalOffset > 0) {
        insnList.add(
            new SUBInsn(
                Condition.AL,
                false,
                Register.SP,
                Register.SP,
                new Operand2.Immediate(stackDesc.totalOffset)));
      }

      // Store arguments to stack
      for (int i = 0; i < 4 && i < method.args().size(); i++) {
        final var arg = method.args().get(i);
        final var offset = stackDesc.varToLocation.get(arg.id()).offset();
        insnList.add(
            new STRInsn(
                Condition.AL,
                Size.WORD,
                Register.fromInt(i),
                new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)));
      }

      for (final var stmt : method.stmtList()) {
        final var stmtChunk = gen(stmt, stackDesc.varToLocation, stringGen, typeMap);
        if (stmtChunk != null) insnList.addAll(stmtChunk);
      }

      if (stackDesc.totalOffset > 0) {
        insnList.add(
            new ADDInsn(
                Condition.AL,
                false,
                Register.SP,
                Register.SP,
                new Operand2.Immediate(stackDesc.totalOffset)));
      }

      final var finalInsn = insnList.get(insnList.size() - 1);
      if (finalInsn instanceof LDMFDInsn ldmfdInsn && ldmfdInsn.registers().contains(Register.PC)) {
        continue;
      }
      insnList.add(
          new LDMFDInsn(
              Register.SP, EnumSet.of(Register.R4, Register.R5, Register.R6, Register.PC), true));
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
          new AssemblerDirective("ascii", "\"" + StringEscapeUtils.escapeJava(string) + "\\000\""));
    }

    return new Program(insnList);
  }

  private record StackDescriptor(
      Map<String, LocationDescriptor.Stack> varToLocation, int totalOffset) {}

  private static StackDescriptor buildStackDesc(Method method) {
    final var result = new HashMap<String, LocationDescriptor.Stack>();
    int offset;
    offset = -16;
    for (int i = method.args().size() - 1; i >= 4; i--) {
      final var v = method.args().get(i);
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset -= 4;
    }
    offset = 0;
    for (int i = 0; i < 4 && i < method.args().size(); i++) {
      final var v = method.args().get(i);
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    for (final var v : method.vars()) {
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    return new StackDescriptor(Collections.unmodifiableMap(result), offset);
  }

  private static List<Insn> gen(
      Stmt stmt,
      Map<String, LocationDescriptor.Stack> stackDesc,
      StringGen stringGen,
      Map<String, Type> typeMap) {
    return switch (stmt.getStmtType()) {
      case LABEL -> {
        final var ls = (LabelStmt) stmt;
        yield List.of(new LabelInsn(ls.label()));
      }
      case CMP -> {
        final var cs = (CmpStmt) stmt;
        final var condition = cs.condition();
        if (condition instanceof IdRvalExpr ce) {
          final var offset = stackDesc.get(ce.id()).offset();
          yield List.of(
              new LDRInsn(
                  Condition.AL,
                  Size.WORD,
                  Register.R4,
                  new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)),
              new CMPInsn(Condition.AL, Register.R4, new Operand2.Immediate(0)),
              new BInsn(Condition.NE, cs.dest().label()));
        } else if (condition instanceof BinaryExpr be) {
          final var cond =
              switch (be.op()) {
                case EQ -> Condition.EQ;
                case LT -> Condition.LT;
                case GT -> Condition.GT;
                case AND, OR, MULT, DIV, PLUS, MINUS -> throw new RuntimeException(
                    "IR3 if statements contains op not in RelExp");
                case LEQ -> Condition.LE;
                case GEQ -> Condition.GE;
                case NEQ -> Condition.NE;
              };
          final var lhsInsn = loadRvalExpr(be.lhs(), Register.R4, stackDesc, stringGen);
          final var rhsInsn = loadRvalExpr(be.rhs(), Register.R5, stackDesc, stringGen);
          yield List.of(
              lhsInsn,
              rhsInsn,
              new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
              new BInsn(cond, cs.dest().label()));
        } else {
          throw new RuntimeException(
              "IR3 if statements can only contain Binary Expression or idc3");
        }
      }
      case GOTO -> {
        final var gs = (GotoStmt) stmt;
        yield List.of(new BInsn(Condition.AL, gs.dest().label()));
      }
      case READLN -> null;
      case PRINTLN -> {
        final var ps = (PrintlnStmt) stmt;
        final var rval = ps.rval();
        yield switch (rval.getRvalExprType()) {
          case BOOL -> {
            final var boole = (BoolRvalExpr) rval;
            final var label = stringGen.gen(boole.value() ? "true" : "false");
            yield List.of(
                new LDRInsn(
                    Condition.AL, Size.WORD, Register.R0, new MemoryAddress.PCRelative(label)),
                new BLInsn(Condition.AL, "puts"));
          }
          case NULL -> List.of(
              new LDRInsn(
                  Condition.AL,
                  Size.WORD,
                  Register.R0,
                  new MemoryAddress.PCRelative(stringGen.gen(""))),
              new BLInsn(Condition.AL, "puts"));
          case STRING -> {
            final var se = (StringRvalExpr) rval;
            final var label = stringGen.gen(se.value());
            yield List.of(
                new LDRInsn(
                    Condition.AL, Size.WORD, Register.R0, new MemoryAddress.PCRelative(label)),
                new BLInsn(Condition.AL, "puts"));
          }
          case INT -> {
            final var ie = (IntRvalExpr) rval;
            final var label = stringGen.gen(Integer.toString(ie.value()));
            yield List.of(
                new LDRInsn(
                    Condition.AL, Size.WORD, Register.R0, new MemoryAddress.PCRelative(label)),
                new BLInsn(Condition.AL, "puts"));
          }
          case ID -> {
            final var ie = (IdRvalExpr) rval;
            final var offset = stackDesc.get(ie.id()).offset();
            yield switch (typeMap.get(ie.id()).type()) {
              case INT -> {
                final var formatLabel = stringGen.gen("%d\n");
                yield List.of(
                    new LDRInsn(
                        Condition.AL,
                        Size.WORD,
                        Register.R0,
                        new MemoryAddress.PCRelative(formatLabel)),
                    new LDRInsn(
                        Condition.AL,
                        Size.WORD,
                        Register.R1,
                        new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)),
                    new BLInsn(Condition.AL, "printf"));
              }
              case BOOL -> {
                final var trueLabel = stringGen.gen("true");
                final var falseLabel = stringGen.gen("false");
                yield List.of(
                    new LDRInsn(
                        Condition.AL,
                        Size.WORD,
                        Register.R4,
                        new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)),
                    new CMPInsn(Condition.AL, Register.R4, new Operand2.Immediate(0)),
                    new LDRInsn(
                        Condition.NE,
                        Size.WORD,
                        Register.R0,
                        new MemoryAddress.PCRelative(trueLabel)),
                    new LDRInsn(
                        Condition.EQ,
                        Size.WORD,
                        Register.R0,
                        new MemoryAddress.PCRelative(falseLabel)),
                    new BLInsn(Condition.AL, "puts"));
              }
              case STRING -> {
                final var formatLabel = stringGen.gen("%s\n");
                yield List.of(
                    new LDRInsn(
                        Condition.AL,
                        Size.WORD,
                        Register.R0,
                        new MemoryAddress.PCRelative(formatLabel)),
                    new LDRInsn(
                        Condition.AL,
                        Size.WORD,
                        Register.R1,
                        new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)),
                    new BLInsn(Condition.AL, "printf"));
              }
              case VOID, CLASS -> throw new RuntimeException("Should not have passed typechecking");
            };
          }
        };
      }
      case VAR_ASSIGN -> {
        final var vas = (VarAssignStmt) stmt;
        final var offset = stackDesc.get(vas.lhs().id()).offset();
        final var rhsExpr = loadExpr(vas.rhs(), Register.R4, stackDesc, stringGen);
        yield ImmutableList.<Insn>builder()
            .addAll(rhsExpr)
            .add(
                new STRInsn(
                    Condition.AL,
                    Size.WORD,
                    Register.R4,
                    new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)))
            .build();
      }
      case FIELD_ASSIGN -> null;
      case CALL -> null;
      case RETURN -> {
        final var rs = (ReturnStmt) stmt;
        if (rs.maybeValue().isEmpty()) {
          yield List.of(
              new LDMFDInsn(
                  Register.SP,
                  EnumSet.of(Register.R4, Register.R5, Register.R6, Register.PC),
                  true));
        }
        final var value = rs.maybeValue().get();
        final var offset = stackDesc.get(value.id()).offset();
        yield List.of(
            new LDRInsn(
                Condition.AL,
                Size.WORD,
                Register.R0,
                new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)),
            new LDMFDInsn(
                Register.SP, EnumSet.of(Register.R4, Register.R5, Register.R6, Register.PC), true));
      }
    };
  }

  private static List<Insn> loadExpr(
      Expr expr,
      Register dest,
      Map<String, LocationDescriptor.Stack> stackDesc,
      StringGen stringGen) {
    return switch (expr.getExprType()) {
      case BINARY -> {
        final var be = (BinaryExpr) expr;
        final var lhs = loadRvalExpr(be.lhs(), Register.R4, stackDesc, stringGen);
        final var rhs = loadRvalExpr(be.rhs(), Register.R5, stackDesc, stringGen);
        final List<Insn> insnList =
            switch (be.op()) {
              case PLUS -> List.of(
                  new ADDInsn(
                      Condition.AL, false, dest, Register.R4, new Operand2.Register(Register.R5)));
              case MINUS -> List.of(
                  new SUBInsn(
                      Condition.AL, false, dest, Register.R4, new Operand2.Register(Register.R5)));
              case LT -> List.of(
                  new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                  new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
                  new MOVInsn(Condition.LT, dest, new Operand2.Immediate(1)));
              case GT -> List.of(
                  new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                  new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
                  new MOVInsn(Condition.GT, dest, new Operand2.Immediate(1)));
              case LEQ -> List.of(
                  new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                  new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
                  new MOVInsn(Condition.LE, dest, new Operand2.Immediate(1)));
              case GEQ -> List.of(
                  new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                  new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
                  new MOVInsn(Condition.GE, dest, new Operand2.Immediate(1)));
              case EQ -> List.of(
                  new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                  new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
                  new MOVInsn(Condition.EQ, dest, new Operand2.Immediate(1)));
              case NEQ -> List.of(
                  new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0)),
                  new CMPInsn(Condition.AL, Register.R4, new Operand2.Register(Register.R5)),
                  new MOVInsn(Condition.NE, dest, new Operand2.Immediate(1)));
              case OR -> List.of(
                  new ORRInsn(
                      Condition.AL, false, dest, Register.R4, new Operand2.Register(Register.R5)));
              case AND -> List.of(
                  new ANDInsn(
                      Condition.AL, false, dest, Register.R4, new Operand2.Register(Register.R5)));
              case MULT -> List.of(
                  new MULInsn(Condition.AL, false, dest, Register.R4, Register.R5));
              case DIV -> List.of(new SDIVInsn(Condition.AL, dest, Register.R4, Register.R5));
            };
        yield ImmutableList.<Insn>builder().add(lhs).add(rhs).addAll(insnList).build();
      }
      case UNARY -> {
        final var ue = (UnaryExpr) expr;
        final var loadInsn = loadRvalExpr(ue.rval(), Register.R4, stackDesc, stringGen);
        final var unaryInsn =
            switch (ue.op()) {
              case NOT -> new MVNInsn(Condition.AL, dest, new Operand2.Register(Register.R4));
              case NEGATIVE -> new RSBInsn(
                  Condition.AL, false, dest, Register.R4, new Operand2.Immediate(0));
            };
        yield List.of(loadInsn, unaryInsn);
      }
      case FIELD -> null;
      case RVAL -> List.of(loadRvalExpr((RvalExpr) expr, dest, stackDesc, stringGen));
      case CALL -> null;
      case NEW -> null;
    };
  }

  private static ARMInsn loadRvalExpr(
      RvalExpr rvalExpr,
      Register dest,
      Map<String, LocationDescriptor.Stack> stackDesc,
      StringGen stringGen) {
    return switch (rvalExpr.getRvalExprType()) {
      case ID -> {
        final var ie = (IdRvalExpr) rvalExpr;
        final var offset = stackDesc.get(ie.id()).offset();
        yield new LDRInsn(
            Condition.AL,
            Size.WORD,
            dest,
            new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false));
      }
      case BOOL -> {
        final var boole = (BoolRvalExpr) rvalExpr;
        yield new MOVInsn(Condition.AL, dest, new Operand2.Immediate(boole.value() ? 1 : 0));
      }
      case INT -> {
        final var ie = (IntRvalExpr) rvalExpr;
        yield new MOVInsn(Condition.AL, dest, new Operand2.Immediate(ie.value()));
      }
      case STRING -> {
        final var se = (StringRvalExpr) rvalExpr;
        final var label = stringGen.gen(se.value());
        yield new LDRInsn(Condition.AL, Size.WORD, dest, new MemoryAddress.PCRelative(label));
      }
      case NULL -> new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0));
    };
  }
}
