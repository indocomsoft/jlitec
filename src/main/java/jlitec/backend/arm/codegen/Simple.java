package jlitec.backend.arm.codegen;

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
import jlitec.backend.arm.insn.BInsn;
import jlitec.backend.arm.insn.BLInsn;
import jlitec.backend.arm.insn.CMPInsn;
import jlitec.backend.arm.insn.LDMFDInsn;
import jlitec.backend.arm.insn.LDRInsn;
import jlitec.backend.arm.insn.LabelInsn;
import jlitec.backend.arm.insn.MOVInsn;
import jlitec.backend.arm.insn.STMFDInsn;
import jlitec.backend.arm.insn.STRInsn;
import jlitec.backend.arm.insn.SUBInsn;
import jlitec.ir3.Method;
import jlitec.ir3.Var;
import jlitec.ir3.expr.BinaryExpr;
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
        final var stmtChunk = gen(stmt, stackDesc.varToLocation, stringGen);
        if (stmtChunk != null) insnList.addAll(stmtChunk);
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
      Stmt stmt, Map<String, LocationDescriptor.Stack> stackDesc, StringGen stringGen) {
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
              new BInsn(Condition.EQ, cs.dest().label()));
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
        yield List.of(
            loadRvalExpr(ps.rval(), Register.R0, stackDesc, stringGen, true),
            new BLInsn(Condition.AL, "printf"));
      }
      case VAR_ASSIGN -> null;
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

  private static ARMInsn loadRvalExpr(
      RvalExpr rvalExpr,
      Register dest,
      Map<String, LocationDescriptor.Stack> stackDesc,
      StringGen stringGen) {
    return loadRvalExpr(rvalExpr, dest, stackDesc, stringGen, false);
  }

  private static ARMInsn loadRvalExpr(
      RvalExpr rvalExpr,
      Register dest,
      Map<String, LocationDescriptor.Stack> stackDesc,
      StringGen stringGen,
      boolean addNewLineToString) {
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
        yield new MOVInsn(Condition.AL, dest, new Operand2.Immediate(boole.value() ? 0 : 1));
      }
      case INT -> {
        final var ie = (IntRvalExpr) rvalExpr;
        yield new MOVInsn(Condition.AL, dest, new Operand2.Immediate(ie.value()));
      }
      case STRING -> {
        final var se = (StringRvalExpr) rvalExpr;
        final var label = stringGen.gen(se.value() + (addNewLineToString ? "\n" : ""));
        yield new LDRInsn(Condition.AL, Size.WORD, dest, new MemoryAddress.PCRelative(label));
      }
      case NULL -> new MOVInsn(Condition.AL, dest, new Operand2.Immediate(0));
    };
  }
}
