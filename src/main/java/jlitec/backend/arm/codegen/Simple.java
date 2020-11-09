package jlitec.backend.arm.codegen;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import jlitec.ir3.Data;
import jlitec.ir3.Ir3Type;
import jlitec.ir3.Method;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.expr.BinaryExpr;
import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.CallExpr;
import jlitec.ir3.expr.Expr;
import jlitec.ir3.expr.FieldExpr;
import jlitec.ir3.expr.NewExpr;
import jlitec.ir3.expr.UnaryExpr;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;
import jlitec.ir3.stmt.CallStmt;
import jlitec.ir3.stmt.CmpStmt;
import jlitec.ir3.stmt.FieldAssignStmt;
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
    final var dataMap =
        program.dataList().stream()
            .collect(Collectors.toUnmodifiableMap(Data::cname, Function.identity()));
    for (final var method : program.methodList()) {
      final var typeMap =
          Stream.concat(method.argsWithThis().stream(), method.vars().stream())
              .collect(Collectors.toUnmodifiableMap(Var::id, Var::type));
      final var stackDesc = buildStackDesc(method);

      insnList.add(new AssemblerDirective("global", method.id().equals("main") ? "main" : "func"));
      insnList.add(new AssemblerDirective("type", method.id().replace("%", "") + ", %function"));
      insnList.add(new LabelInsn(method.id().replace("%", "")));

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
      for (int i = 0; i < 4 && i < method.argsWithThis().size(); i++) {
        final var arg = method.argsWithThis().get(i);
        final var offset = stackDesc.varToLocation.get(arg.id()).offset();
        insnList.add(
            new STRInsn(
                Condition.AL,
                Size.WORD,
                Register.fromInt(i),
                new MemoryAddress.ImmediateOffset(Register.SP, offset)));
      }

      for (final var stmt : method.stmtList()) {
        final var stmtChunk = gen(stmt, stackDesc, stringGen, typeMap, dataMap);
        if (stmtChunk != null) insnList.addAll(stmtChunk);
      }

      final var finalInsn = insnList.get(insnList.size() - 1);
      if (finalInsn instanceof LDMFDInsn ldmfdInsn && ldmfdInsn.registers().contains(Register.PC)) {
        continue;
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

      insnList.add(
          new LDMFDInsn(
              Register.SP, EnumSet.of(Register.R4, Register.R5, Register.R6, Register.PC), true));
    }

    // getline_without_newline helper, with the equivalent C as comment.
    /*
    getline_without_newline:
      STMFD SP!, {R4, LR}
      SUB SP, SP, #8
      MOV R4, #0
      STR R4, [SP]
      STR R4, [SP, #4]
      LDR R3, .Lstdin
      LDR R2, [R3]
      BL getline
      SUB R1, R0, #1
      LDR R0, [SP]
      STRB R4, [R0, R1]
      BL realloc
      ADD SP, SP, #8
      LDMFD SP!, {R4, PC}
     */
    // getline_without_newline() {
    insnList.add(new LabelInsn("getline_without_newline"));
    insnList.add(new STMFDInsn(Register.SP, EnumSet.of(Register.R4, Register.LR), true));
    insnList.add(
        new SUBInsn(Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(8)));
    insnList.add(new MOVInsn(Condition.AL, Register.R4, new Operand2.Immediate(0)));
    // char* result = NULL;
    insnList.add(
        new STRInsn(
            Condition.AL, Size.WORD, Register.R4, new MemoryAddress.ImmediateOffset(Register.SP)));
    // size_t n = 0;
    insnList.add(
        new STRInsn(
            Condition.AL,
            Size.WORD,
            Register.R4,
            new MemoryAddress.ImmediateOffset(Register.SP, 4)));
    // ssize_t len = getline(&result, &n, stdin);
    insnList.add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Register(Register.PC)));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.R1, Register.R0, new Operand2.Immediate(4)));
    insnList.add(
        new LDRInsn(Condition.AL, Size.WORD, Register.R3, new MemoryAddress.PCRelative(".Lstdin")));
    insnList.add(
        new LDRInsn(
            Condition.AL, Size.WORD, Register.R2, new MemoryAddress.ImmediateOffset(Register.R3)));
    insnList.add(new BLInsn(Condition.AL, "getline"));
    // len = len - 1;
    insnList.add(
        new SUBInsn(Condition.AL, false, Register.R1, Register.R0, new Operand2.Immediate(1)));
    // result[len] = 0;
    insnList.add(
        new LDRInsn(
            Condition.AL, Size.WORD, Register.R0, new MemoryAddress.ImmediateOffset(Register.SP)));
    insnList.add(
        new STRInsn(
            Condition.AL,
            Size.B,
            Register.R4,
            new MemoryAddress.RegisterOffset(Register.R0, Register.R1)));
    // return realloc(result, len - 1);
    insnList.add(new BLInsn(Condition.AL, "realloc"));
    insnList.add(
        new ADDInsn(Condition.AL, false, Register.SP, Register.SP, new Operand2.Immediate(8)));
    insnList.add(new LDMFDInsn(Register.SP, EnumSet.of(Register.R4, Register.PC), true));

    // stdin stream
    insnList.add(new LabelInsn(".Lstdin"));
    insnList.add(new AssemblerDirective("word", "stdin"));

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
    int offset = 0;
    for (int i = 0; i < 4 && i < method.argsWithThis().size(); i++) {
      final var v = method.argsWithThis().get(i);
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    for (final var v : method.vars()) {
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    final var totalOffset = offset;
    offset += 16; // For r4, r5, r6, lr
    for (int i = 4; i < method.argsWithThis().size(); i++) {
      final var v = method.argsWithThis().get(i);
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    return new StackDescriptor(Collections.unmodifiableMap(result), totalOffset);
  }

  private static List<Insn> gen(
      Stmt stmt,
      StackDescriptor stackDescriptor,
      StringGen stringGen,
      Map<String, Type> typeMap,
      Map<String, Data> dataMap) {
    final var stackDesc = stackDescriptor.varToLocation;
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
                  new MemoryAddress.ImmediateOffset(Register.SP, offset)),
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
                        new MemoryAddress.ImmediateOffset(Register.SP, offset)),
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
                        new MemoryAddress.ImmediateOffset(Register.SP, offset)),
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
                        new MemoryAddress.ImmediateOffset(Register.SP, offset)),
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
        final var rhsExpr =
            loadExpr(vas.rhs(), Register.R4, stackDesc, stringGen, dataMap, typeMap);
        yield ImmutableList.<Insn>builder()
            .addAll(rhsExpr)
            .add(
                new STRInsn(
                    Condition.AL,
                    Size.WORD,
                    Register.R4,
                    new MemoryAddress.ImmediateOffset(Register.SP, offset)))
            .build();
      }
      case FIELD_ASSIGN -> {
        final var fas = (FieldAssignStmt) stmt;
        final var cname = ((Type.KlassType) typeMap.get(fas.lhsId().id())).cname();
        final var data = dataMap.get(cname);
        final var maybeFieldOffset =
            IntStream.range(0, data.fields().size())
                .filter(i -> data.fields().get(i).id().equals(fas.lhsField()))
                .map(i -> i * 4)
                .findFirst();
        if (maybeFieldOffset.isEmpty()) {
          throw new RuntimeException("Field not found, should have failed typechecking");
        }
        final var fieldOffset = maybeFieldOffset.getAsInt();
        final var offset = stackDesc.get(fas.lhsId().id()).offset();
        yield ImmutableList.<Insn>builder()
            .addAll(loadExpr(fas.rhs(), Register.R4, stackDesc, stringGen, dataMap, typeMap))
            .add(
                new LDRInsn(
                    Condition.AL,
                    Size.WORD,
                    Register.R5,
                    new MemoryAddress.ImmediateOffset(Register.SP, offset)))
            .add(
                new STRInsn(
                    Condition.AL,
                    Size.WORD,
                    Register.R4,
                    new MemoryAddress.ImmediateOffset(Register.R5, fieldOffset)))
            .build();
      }
      case CALL -> {
        final var ce = (CallStmt) stmt;
        final var regArgsInsn =
            IntStream.range(0, Math.min(4, ce.args().size()))
                .boxed()
                .map(i -> loadRvalExpr(ce.args().get(i), Register.fromInt(i), stackDesc, stringGen))
                .collect(Collectors.toUnmodifiableList());
        final var builder = ImmutableList.<Insn>builder().addAll(regArgsInsn);
        final var numStackArgs = Math.max(0, ce.args().size() - 4);
        if (numStackArgs != 0) {
          builder.add(
              new SUBInsn(
                  Condition.AL,
                  false,
                  Register.SP,
                  Register.SP,
                  new Operand2.Immediate(numStackArgs * 4)));
          final var stackArgsInsn =
              IntStream.range(4, ce.args().size())
                  .boxed()
                  .flatMap(
                      i ->
                          Stream.of(
                              loadRvalExpr(ce.args().get(i), Register.R4, stackDesc, stringGen),
                              new STRInsn(
                                  Condition.AL,
                                  Size.WORD,
                                  Register.R4,
                                  new MemoryAddress.ImmediateOffset(Register.SP, (i - 4) * 4))))
                  .collect(Collectors.toUnmodifiableList());
          builder.addAll(stackArgsInsn);
        }
        builder.add(new BLInsn(Condition.AL, ce.target().id()));
        if (numStackArgs != 0) {
          builder.add(
              new ADDInsn(
                  Condition.AL,
                  false,
                  Register.SP,
                  Register.SP,
                  new Operand2.Immediate(numStackArgs * 4)));
        }
        yield builder.build();
      }
      case RETURN -> {
        final var rs = (ReturnStmt) stmt;
        final var builder = ImmutableList.<Insn>builder();
        if (rs.maybeValue().isPresent()) {
          final var value = rs.maybeValue().get();
          final var offset = stackDesc.get(value.id()).offset();
          builder.add(
              new LDRInsn(
                  Condition.AL,
                  Size.WORD,
                  Register.R0,
                  new MemoryAddress.ImmediateOffset(Register.SP, offset)));
        }
        if (stackDescriptor.totalOffset > 0) {
          builder.add(
              new ADDInsn(
                  Condition.AL,
                  false,
                  Register.SP,
                  Register.SP,
                  new Operand2.Immediate(stackDescriptor.totalOffset)));
        }
        builder.add(
            new LDMFDInsn(
                Register.SP, EnumSet.of(Register.R4, Register.R5, Register.R6, Register.PC), true));
        yield builder.build();
      }
    };
  }

  private record StringChunk(Insn insn, Optional<Integer> maybeLength) {}

  private static List<Insn> loadExpr(
      Expr expr,
      Register dest,
      Map<String, LocationDescriptor.Stack> stackDesc,
      StringGen stringGen,
      Map<String, Data> dataMap,
      Map<String, Type> typeMap) {
    return switch (expr.getExprType()) {
      case BINARY -> {
        final var be = (BinaryExpr) expr;
        if (be.op() == BinaryOp.PLUS) {
          final var isLhsString =
              switch (be.lhs().getRvalExprType()) {
                case BOOL, INT -> false;
                case STRING, NULL -> true;
                case ID -> typeMap.get(((IdRvalExpr) be.lhs()).id()).type() == Ir3Type.STRING;
              };
          if (isLhsString) {
            final StringChunk lhsInsn =
                switch (be.lhs().getRvalExprType()) {
                  case BOOL, INT -> throw new RuntimeException("should not be reached");
                  case STRING -> {
                    final var sre = (jlitec.ir3.expr.rval.StringRvalExpr) be.lhs();
                    final var label = stringGen.gen(sre.value());
                    yield new StringChunk(
                        new LDRInsn(
                            Condition.AL,
                            Size.WORD,
                            Register.R1,
                            new MemoryAddress.PCRelative(label)),
                        Optional.of(sre.value().length() + 1));
                  }
                  case NULL -> new StringChunk(
                      new LDRInsn(
                          Condition.AL,
                          Size.WORD,
                          Register.R1,
                          new MemoryAddress.PCRelative(stringGen.gen(""))),
                      Optional.of(1));
                  case ID -> {
                    final var ire = (IdRvalExpr) be.lhs();
                    final var offset = stackDesc.get(ire.id()).offset();
                    yield new StringChunk(
                        new LDRInsn(
                            Condition.AL,
                            Size.WORD,
                            Register.R1,
                            new MemoryAddress.ImmediateOffset(Register.SP, offset)),
                        Optional.empty());
                  }
                };
            final StringChunk rhsInsn =
                switch (be.rhs().getRvalExprType()) {
                  case BOOL, INT -> throw new RuntimeException("should not be reached");
                  case STRING -> {
                    final var sre = (jlitec.ir3.expr.rval.StringRvalExpr) be.rhs();
                    final var label = stringGen.gen(sre.value());
                    yield new StringChunk(
                        new LDRInsn(
                            Condition.AL,
                            Size.WORD,
                            Register.R1,
                            new MemoryAddress.PCRelative(label)),
                        Optional.of(sre.value().length() + 1));
                  }
                  case NULL -> new StringChunk(
                      new LDRInsn(
                          Condition.AL,
                          Size.WORD,
                          Register.R1,
                          new MemoryAddress.PCRelative(stringGen.gen(""))),
                      Optional.of(1));
                  case ID -> {
                    final var ire = (IdRvalExpr) be.rhs();
                    final var offset = stackDesc.get(ire.id()).offset();
                    yield new StringChunk(
                        new LDRInsn(
                            Condition.AL,
                            Size.WORD,
                            Register.R1,
                            new MemoryAddress.ImmediateOffset(Register.SP, offset)),
                        Optional.empty());
                  }
                };
            final Optional<Integer> maybeLength =
                lhsInsn.maybeLength.flatMap(l -> rhsInsn.maybeLength.map(r -> l + r - 1));
            final var builder = ImmutableList.<Insn>builder();
            if (maybeLength.isPresent()) {
              final var length = maybeLength.get();
              builder
                  .add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Immediate(length)))
                  .add(new BLInsn(Condition.AL, "malloc"))
                  .add(lhsInsn.insn)
                  .add(new BLInsn(Condition.AL, "strcpy"))
                  .add(rhsInsn.insn)
                  .add(new BLInsn(Condition.AL, "strcat"));
            } else {
              // Fill in R4 with lhs length, (potentially) R6 with lhs string
              if (lhsInsn.maybeLength.isEmpty()) {
                builder
                    .add(lhsInsn.insn)
                    .add(new MOVInsn(Condition.AL, Register.R6, new Operand2.Register(Register.R1)))
                    .add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Register(Register.R6)))
                    .add(new BLInsn(Condition.AL, "strlen"))
                    .add(
                        new MOVInsn(Condition.AL, Register.R0, new Operand2.Register(Register.R4)));
              } else {
                builder.add(
                    new MOVInsn(
                        Condition.AL,
                        Register.R4,
                        new Operand2.Immediate(lhsInsn.maybeLength.get())));
              }
              // Fill in R0 with the rhs length, (potentially) R5 with the rhs string
              if (rhsInsn.maybeLength.isEmpty()) {
                builder
                    .add(rhsInsn.insn)
                    .add(new MOVInsn(Condition.AL, Register.R5, new Operand2.Register(Register.R1)))
                    .add(new MOVInsn(Condition.AL, Register.R0, new Operand2.Register(Register.R5)))
                    .add(new BLInsn(Condition.AL, "strlen"));
              } else {
                builder.add(
                    new MOVInsn(
                        Condition.AL,
                        Register.R0,
                        new Operand2.Immediate(rhsInsn.maybeLength.get())));
              }
              builder
                  .add(
                      new ADDInsn(
                          Condition.AL,
                          false,
                          Register.R0,
                          Register.R0,
                          new Operand2.Register(Register.R4)))
                  .add(new BLInsn(Condition.AL, "malloc"));

              if (lhsInsn.maybeLength.isEmpty()) {
                // R6 is filled in with lhs string
                builder
                    .add(new MOVInsn(Condition.AL, Register.R1, new Operand2.Register(Register.R6)))
                    .add(new BLInsn(Condition.AL, "strcpy"));
              } else {
                builder.add(lhsInsn.insn).add(new BLInsn(Condition.AL, "strcpy"));
              }
              if (rhsInsn.maybeLength.isEmpty()) {
                // R5 is filled in with rhs string
                builder
                    .add(new MOVInsn(Condition.AL, Register.R1, new Operand2.Register(Register.R5)))
                    .add(new BLInsn(Condition.AL, "strcat"));
              } else {
                builder.add(rhsInsn.insn).add(new BLInsn(Condition.AL, "strcat"));
              }
            }
            yield builder
                .add(new MOVInsn(Condition.AL, dest, new Operand2.Register(Register.R0)))
                .build();
          }
        }

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
      case FIELD -> {
        final var fas = (FieldExpr) expr;
        if (typeMap.get(fas.target().id()) instanceof Type.PrimitiveType) {
          System.out.println(fas.target().id());
          System.out.println(typeMap);
        }
        final var cname = ((Type.KlassType) typeMap.get(fas.target().id())).cname();
        final var data = dataMap.get(cname);
        final var maybeFieldOffset =
            IntStream.range(0, data.fields().size())
                .filter(i -> data.fields().get(i).id().equals(fas.field()))
                .map(i -> i * 4)
                .findFirst();
        if (maybeFieldOffset.isEmpty()) {
          throw new RuntimeException("Field not found, should have failed typechecking");
        }
        final var fieldOffset = maybeFieldOffset.getAsInt();
        final var offset = stackDesc.get(fas.target().id()).offset();
        yield List.of(
            new LDRInsn(
                Condition.AL,
                Size.WORD,
                Register.R4,
                new MemoryAddress.ImmediateOffset(Register.SP, offset)),
            new LDRInsn(
                Condition.AL,
                Size.WORD,
                dest,
                new MemoryAddress.ImmediateOffset(Register.R4, fieldOffset)));
      }
      case RVAL -> List.of(loadRvalExpr((RvalExpr) expr, dest, stackDesc, stringGen));
      case CALL -> {
        final var ce = (CallExpr) expr;
        final var regArgsInsn =
            IntStream.range(0, Math.min(4, ce.args().size()))
                .boxed()
                .map(i -> loadRvalExpr(ce.args().get(i), Register.fromInt(i), stackDesc, stringGen))
                .collect(Collectors.toUnmodifiableList());
        final var builder = ImmutableList.<Insn>builder().addAll(regArgsInsn);
        final var numStackArgs = Math.max(0, ce.args().size() - 4);
        if (numStackArgs != 0) {
          builder.add(
              new SUBInsn(
                  Condition.AL,
                  false,
                  Register.SP,
                  Register.SP,
                  new Operand2.Immediate(numStackArgs * 4)));
          final var stackArgsInsn =
              IntStream.range(4, ce.args().size())
                  .boxed()
                  .flatMap(
                      i ->
                          Stream.of(
                              loadRvalExpr(ce.args().get(i), Register.R4, stackDesc, stringGen),
                              new STRInsn(
                                  Condition.AL,
                                  Size.WORD,
                                  Register.R4,
                                  new MemoryAddress.ImmediateOffset(Register.SP, (i - 4) * 4))))
                  .collect(Collectors.toUnmodifiableList());
          builder.addAll(stackArgsInsn);
        }
        builder.add(new BLInsn(Condition.AL, ce.target().id()));
        builder.add(new MOVInsn(Condition.AL, dest, new Operand2.Register(Register.R0)));
        if (numStackArgs != 0) {
          builder.add(
              new ADDInsn(
                  Condition.AL,
                  false,
                  Register.SP,
                  Register.SP,
                  new Operand2.Immediate(numStackArgs * 4)));
        }
        yield builder.build();
      }
      case NEW -> {
        final var ne = (NewExpr) expr;
        final var data = dataMap.get(ne.cname());
        yield List.of(
            new MOVInsn(
                Condition.AL, Register.R0, new Operand2.Immediate(data.fields().size() * 4)),
            new BLInsn(Condition.AL, "malloc"),
            new MOVInsn(Condition.AL, dest, new Operand2.Register(Register.R0)));
      }
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
            Condition.AL, Size.WORD, dest, new MemoryAddress.ImmediateOffset(Register.SP, offset));
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
