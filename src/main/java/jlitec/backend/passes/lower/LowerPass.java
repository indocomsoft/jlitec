package jlitec.backend.passes.lower;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.BitOp;
import jlitec.backend.passes.lower.stmt.BranchLinkLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAssignLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LabelLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadLargeImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadStackArgLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.PopStackLowerStmt;
import jlitec.backend.passes.lower.stmt.PushPadStackLowerStmt;
import jlitec.backend.passes.lower.stmt.PushStackLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ReturnLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;
import jlitec.ir3.Data;
import jlitec.ir3.Ir3Type;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.codegen.TempVarGen;
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
import jlitec.ir3.expr.rval.LiteralRvalExpr;
import jlitec.ir3.expr.rval.NullRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.expr.rval.RvalExprType;
import jlitec.ir3.expr.rval.StringRvalExpr;
import jlitec.ir3.stmt.CallStmt;
import jlitec.ir3.stmt.CmpStmt;
import jlitec.ir3.stmt.FieldAssignStmt;
import jlitec.ir3.stmt.GotoStmt;
import jlitec.ir3.stmt.LabelStmt;
import jlitec.ir3.stmt.PrintlnStmt;
import jlitec.ir3.stmt.ReadlnStmt;
import jlitec.ir3.stmt.ReturnStmt;
import jlitec.ir3.stmt.Stmt;
import jlitec.ir3.stmt.VarAssignStmt;

public class LowerPass implements Pass<jlitec.ir3.Program, Program> {
  @Override
  public Program pass(jlitec.ir3.Program input) {
    final var cnameToData =
        input.dataList().stream()
            .collect(Collectors.toUnmodifiableMap(Data::cname, Function.identity()));
    final var methodList =
        input.methodList().stream()
            .map(m -> LowerPass.pass(m, cnameToData))
            .collect(Collectors.toUnmodifiableList());
    return new Program(input.dataList(), methodList);
  }

  private static Method pass(jlitec.ir3.Method method, Map<String, Data> cnameToData) {
    final var stmtList = new ArrayList<LowerStmt>();

    final var typeMap =
        Stream.concat(method.argsWithThis().stream(), method.vars().stream())
            .collect(Collectors.toUnmodifiableMap(Var::id, Var::type));
    final var gen = new TempVarGen("__t");

    final var argsWithThis = method.argsWithThis();
    for (int i = 0; i < 4 && i < argsWithThis.size(); i++) {
      final var arg = argsWithThis.get(i);
      stmtList.add(
          new MovLowerStmt(
              new Addressable.IdRval(new IdRvalExpr(arg.id())),
              new Addressable.Reg(Register.fromInt(i))));
    }
    if (argsWithThis.size() > 4) {
      final var loadStackArgLowerStmts =
          argsWithThis.subList(4, argsWithThis.size()).stream()
              .map(LoadStackArgLowerStmt::new)
              .collect(Collectors.toUnmodifiableList());
      stmtList.addAll(loadStackArgLowerStmts);
    }
    for (final var stmt : method.stmtList()) {
      stmtList.addAll(pass(stmt, typeMap, gen, cnameToData));
    }
    final var finalStmt = stmtList.get(stmtList.size() - 1);
    if (!(finalStmt instanceof ReturnLowerStmt)) {
      stmtList.add(new ReturnLowerStmt());
    }

    final var replacedLargeImmStmtList = passReplaceLargeImmediate(stmtList, gen);

    final var vars =
        Stream.concat(method.vars().stream(), gen.getVars().stream())
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(),
        method.id().replace("%", ""),
        argsWithThis,
        vars,
        List.of(),
        replacedLargeImmStmtList);
  }

  private static Optional<Integer> stringyRvalExprLength(RvalExpr e) {
    if (e instanceof StringRvalExpr sre) {
      return Optional.of(sre.value().length());
    } else if (e instanceof NullRvalExpr) {
      return Optional.of(0);
    }
    return Optional.empty();
  }

  private static List<LowerStmt> pass(
      Stmt stmt, Map<String, Type> typeMap, TempVarGen gen, Map<String, Data> cnameToData) {
    return switch (stmt.getStmtType()) {
      case LABEL -> {
        final var ls = (LabelStmt) stmt;
        yield List.of(new LabelLowerStmt(ls.label()));
      }
      case CMP -> {
        final var cs = (CmpStmt) stmt;
        final var condition = cs.condition();
        yield switch (condition.getExprType()) {
          case BINARY -> {
            final var be = (BinaryExpr) condition;
            if (be.lhs() instanceof IdRvalExpr lhs) {
              yield List.of(new CmpLowerStmt(be.op(), lhs, be.rhs(), cs.dest().label()));
            } else if (be.rhs() instanceof IdRvalExpr rhs) {
              yield List.of(
                  new CmpLowerStmt(be.op().comparisonOpposite(), rhs, be.lhs(), cs.dest().label()));
            } else {
              final var tempVar =
                  switch (be.lhs().getRvalExprType()) {
                    case ID, NULL, STRING -> throw new RuntimeException("should not be reached");
                    case INT -> gen.gen(new Type.PrimitiveType(Ir3Type.INT));
                    case BOOL -> gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
                  };
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(idRvalExpr), (LiteralRvalExpr) be.lhs()),
                  new CmpLowerStmt(be.op(), idRvalExpr, be.rhs(), cs.dest().label()));
            }
          }
          case RVAL -> {
            final var re = (RvalExpr) condition;
            yield switch (re.getRvalExprType()) {
              case STRING, NULL, INT -> throw new RuntimeException();
              case ID -> {
                final var ire = (IdRvalExpr) re;
                yield List.of(
                    new CmpLowerStmt(BinaryOp.EQ, ire, new BoolRvalExpr(true), cs.dest().label()));
              }
              case BOOL -> {
                final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                yield List.of(
                    new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), (BoolRvalExpr) re),
                    new CmpLowerStmt(
                        BinaryOp.EQ, idRvalExpr, new BoolRvalExpr(true), cs.dest().label()));
              }
            };
          }
          case NEW, CALL, FIELD, UNARY -> throw new RuntimeException(
              "Invalid IR3 if condition expr type");
        };
      }
      case GOTO -> {
        final var gs = (GotoStmt) stmt;
        yield List.of(new GotoLowerStmt(gs.dest().label()));
      }
      case READLN -> {
        final var rs = (ReadlnStmt) stmt;
        yield switch (typeMap.get(rs.dest().id()).type()) {
          case BOOL -> List.of(
              new BranchLinkLowerStmt("readln_bool"),
              new MovLowerStmt(
                  new Addressable.IdRval(rs.dest()), new Addressable.Reg(Register.R0)));
          case INT -> List.of(
              new BranchLinkLowerStmt("readln_int"),
              new MovLowerStmt(
                  new Addressable.IdRval(rs.dest()), new Addressable.Reg(Register.R0)));
          case STRING -> List.of(
              new BranchLinkLowerStmt("getline_without_newline"),
              new MovLowerStmt(
                  new Addressable.IdRval(rs.dest()), new Addressable.Reg(Register.R0)));
          case VOID, CLASS -> throw new RuntimeException("should have failed typecheck");
        };
      }
      case PRINTLN -> {
        final var ps = (PrintlnStmt) stmt;
        yield switch (ps.rval().getRvalExprType()) {
          case STRING -> List.of(
              new ImmediateLowerStmt(new Addressable.Reg(Register.R0), (StringRvalExpr) ps.rval()),
              new BranchLinkLowerStmt("puts"));
          case NULL -> List.of(
              new ImmediateLowerStmt(new Addressable.Reg(Register.R0), new StringRvalExpr("")),
              new BranchLinkLowerStmt("puts"));
          case INT -> {
            final var ire = (IntRvalExpr) ps.rval();
            yield List.of(
                new ImmediateLowerStmt(
                    new Addressable.Reg(Register.R0),
                    new StringRvalExpr(Integer.toString(ire.value()))),
                new BranchLinkLowerStmt("puts"));
          }
          case BOOL -> {
            final var bre = (BoolRvalExpr) ps.rval();
            yield List.of(
                new ImmediateLowerStmt(
                    new Addressable.Reg(Register.R0),
                    new StringRvalExpr(bre.value() ? "true" : "false")),
                new BranchLinkLowerStmt("puts"));
          }
          case ID -> {
            final var ire = (IdRvalExpr) ps.rval();
            final var movLowerStmt =
                new MovLowerStmt(new Addressable.Reg(Register.R0), new Addressable.IdRval(ire));
            yield switch (typeMap.get(ire.id()).type()) {
              case STRING -> List.of(movLowerStmt, new BranchLinkLowerStmt("puts"));
              case BOOL -> List.of(movLowerStmt, new BranchLinkLowerStmt("println_bool"));
              case INT -> List.of(
                  new ImmediateLowerStmt(
                      new Addressable.Reg(Register.R0), new StringRvalExpr("%d\n")),
                  new MovLowerStmt(new Addressable.Reg(Register.R1), new Addressable.IdRval(ire)),
                  new BranchLinkLowerStmt("printf"));
              case VOID, CLASS -> throw new RuntimeException("should have failed typecheck");
            };
          }
        };
      }
      case VAR_ASSIGN -> {
        final var vas = (VarAssignStmt) stmt;
        yield genAssignToIdRvalExpr(
            vas.rhs(), vas.lhs(), typeMap.get(vas.lhs().id()), typeMap, gen, cnameToData);
      }
      case FIELD_ASSIGN -> {
        final var fas = (FieldAssignStmt) stmt;
        final var cname = ((Type.KlassType) typeMap.get(fas.lhsId().id())).cname();
        final var fieldType =
            cnameToData.get(cname).fields().stream()
                .filter(f -> f.id().equals(fas.lhsField()))
                .findFirst()
                .get()
                .type();
        final var tempVar = gen.gen(fieldType);
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield ImmutableList.<LowerStmt>builder()
            .addAll(
                genAssignToIdRvalExpr(fas.rhs(), idRvalExpr, fieldType, typeMap, gen, cnameToData))
            .add(
                new FieldAssignLowerStmt(
                    fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
            .build();
      }
      case CALL -> {
        final var cs = (CallStmt) stmt;
        yield genCall(cs.args(), cs.target().id(), typeMap, cnameToData, gen);
      }
      case RETURN -> {
        final var rs = (ReturnStmt) stmt;
        final var maybeValue = rs.maybeValue();
        if (maybeValue.isPresent()) {
          final var value = maybeValue.get();
          yield List.of(
              new MovLowerStmt(new Addressable.Reg(Register.R0), new Addressable.IdRval(value)),
              new ReturnLowerStmt());
        }
        yield List.of(new ReturnLowerStmt());
      }
    };
  }

  private static List<LowerStmt> generatePushStackLowerStmt(
      List<RvalExpr> rvalExprList, TempVarGen gen, Map<String, Type> typeMap) {
    final var result = new ArrayList<LowerStmt>();
    if ((rvalExprList.size() & 1) == 1) {
      result.add(new PushPadStackLowerStmt());
    }
    for (final var rvalExpr : Lists.reverse(rvalExprList)) {
      final List<LowerStmt> stmtList =
          switch (rvalExpr.getRvalExprType()) {
            case ID -> List.of(new PushStackLowerStmt((IdRvalExpr) rvalExpr));
            case BOOL -> {
              final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(idRvalExpr), (BoolRvalExpr) rvalExpr),
                  new PushStackLowerStmt(idRvalExpr));
            }
            case INT -> {
              final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(idRvalExpr), (IntRvalExpr) rvalExpr),
                  new PushStackLowerStmt(idRvalExpr));
            }
            case STRING -> {
              final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(idRvalExpr), (StringRvalExpr) rvalExpr),
                  new PushStackLowerStmt(idRvalExpr));
            }
            case NULL -> {
              final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(idRvalExpr), new StringRvalExpr("")),
                  new PushStackLowerStmt(idRvalExpr));
            }
          };
      result.addAll(stmtList);
    }
    return Collections.unmodifiableList(result);
  }

  private record IdRvalExprChunk(IdRvalExpr idRvalExpr, List<LowerStmt> lowerStmtList) {
    public IdRvalExprChunk {
      this.lowerStmtList = Collections.unmodifiableList(lowerStmtList);
    }
  }

  private static IdRvalExprChunk rvaltoIdRval(RvalExpr rvalExpr, TempVarGen gen) {
    return switch (rvalExpr.getRvalExprType()) {
      case ID -> new IdRvalExprChunk((IdRvalExpr) rvalExpr, List.of());
      case BOOL -> {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalExprChunk(
            idRvalExpr,
            List.of(
                new ImmediateLowerStmt(
                    new Addressable.IdRval(idRvalExpr), (BoolRvalExpr) rvalExpr)));
      }
      case INT -> {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalExprChunk(
            idRvalExpr,
            List.of(
                new ImmediateLowerStmt(
                    new Addressable.IdRval(idRvalExpr), (IntRvalExpr) rvalExpr)));
      }
      case STRING -> {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalExprChunk(
            idRvalExpr,
            List.of(
                new ImmediateLowerStmt(
                    new Addressable.IdRval(idRvalExpr), (StringRvalExpr) rvalExpr)));
      }
      case NULL -> {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalExprChunk(
            idRvalExpr,
            List.of(
                new ImmediateLowerStmt(
                    new Addressable.IdRval(idRvalExpr), new StringRvalExpr(""))));
      }
    };
  }

  private static List<LowerStmt> genCall(
      List<RvalExpr> args,
      String target,
      Map<String, Type> typeMap,
      Map<String, Data> cnameToData,
      TempVarGen gen) {
    final var result = new ArrayList<LowerStmt>();
    final var theThis = (IdRvalExpr) args.get(0);
    final var thisCname = ((Type.KlassType) typeMap.get(theThis.id())).cname();
    final var data = cnameToData.get(thisCname);
    if (data.sizeof() != 0) {
      result.add(
          new MovLowerStmt(new Addressable.Reg(Register.R0), new Addressable.IdRval(theThis)));
    }
    for (int i = 1; i < 4 && i < args.size(); i++) {
      final var arg = args.get(i);
      if (arg.getRvalExprType() == RvalExprType.ID) {
        result.add(
            new MovLowerStmt(
                new Addressable.Reg(Register.fromInt(i)),
                new Addressable.IdRval((IdRvalExpr) arg)));
      } else {
        result.add(
            new ImmediateLowerStmt(
                new Addressable.Reg(Register.fromInt(i)), (LiteralRvalExpr) arg));
      }
    }
    final List<RvalExpr> stackArgs = args.size() > 4 ? args.subList(4, args.size()) : List.of();
    if (!stackArgs.isEmpty()) {
      result.addAll(generatePushStackLowerStmt(stackArgs, gen, typeMap));
    }
    result.add(new BranchLinkLowerStmt(target));
    if (!stackArgs.isEmpty()) {
      result.add(new PopStackLowerStmt(stackArgs.size() + ((stackArgs.size() & 1) == 1 ? 1 : 0)));
    }
    return Collections.unmodifiableList(result);
  }

  private static List<LowerStmt> genAssignToIdRvalExpr(
      Expr expr,
      IdRvalExpr dest,
      Type destType,
      Map<String, Type> typeMap,
      TempVarGen gen,
      Map<String, Data> cnameToData) {
    return switch (expr.getExprType()) {
      case BINARY -> {
        final var be = (BinaryExpr) expr;

        if (be.op() == BinaryOp.PLUS) {
          final var isString = destType.type() == Ir3Type.STRING;
          if (isString) {
            // String concatenation
            final var result = new ArrayList<LowerStmt>();
            final Optional<Integer> maybeLhsLength = stringyRvalExprLength(be.lhs());
            final Optional<Integer> maybeRhsLength = stringyRvalExprLength(be.rhs());
            final var maybeTotalLength =
                maybeLhsLength.flatMap(l -> maybeRhsLength.map(r -> l + r + 1));
            // Put the correct length in R0
            if (maybeTotalLength.isPresent()) {
              result.add(
                  new ImmediateLowerStmt(
                      new Addressable.Reg(Register.R0), new IntRvalExpr(maybeTotalLength.get())));
            } else {
              if (maybeLhsLength.isPresent()) {
                final var rhs = (IdRvalExpr) be.rhs();
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R0), new Addressable.IdRval(rhs)));
                result.add(new BranchLinkLowerStmt("strlen"));
                // Plus 1 to account for the null
                result.add(
                    new BinaryLowerStmt(
                        BinaryOp.PLUS,
                        new Addressable.Reg(Register.R0),
                        new Addressable.Reg(Register.R0),
                        new IntRvalExpr(maybeLhsLength.get() + 1)));
              } else if (maybeRhsLength.isPresent()) {
                final var lhs = (IdRvalExpr) be.lhs();
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R0), new Addressable.IdRval(lhs)));
                result.add(new BranchLinkLowerStmt("strlen"));
                // Plus 1 to account for the null
                result.add(
                    new BinaryLowerStmt(
                        BinaryOp.PLUS,
                        new Addressable.Reg(Register.R0),
                        new Addressable.Reg(Register.R0),
                        new IntRvalExpr(maybeRhsLength.get() + 1)));
              } else {
                final var lhsLengthIdRval =
                    new IdRvalExpr(gen.gen(new Type.PrimitiveType(Ir3Type.INT)).id());
                final var lhs = (IdRvalExpr) be.lhs();
                final var rhs = (IdRvalExpr) be.rhs();
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R0), new Addressable.IdRval(lhs)));
                result.add(new BranchLinkLowerStmt("strlen"));
                // Plus 1 to account for the null
                result.add(
                    new BinaryLowerStmt(
                        BinaryOp.PLUS,
                        new Addressable.IdRval(lhsLengthIdRval),
                        new Addressable.Reg(Register.R0),
                        new IntRvalExpr(1)));
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R0), new Addressable.IdRval(rhs)));
                result.add(new BranchLinkLowerStmt("strlen"));
                result.add(
                    new BinaryLowerStmt(
                        BinaryOp.PLUS,
                        new Addressable.Reg(Register.R0),
                        new Addressable.Reg(Register.R0),
                        lhsLengthIdRval));
              }
            }
            // malloc the new string
            result.add(new BranchLinkLowerStmt("malloc"));

            // copy/concatenate lhs and rhs
            if (maybeTotalLength.isPresent()) {
              // Both strings are statically defined
              final var lhs = be.lhs() instanceof StringRvalExpr sre ? sre.value() : "";
              final var rhs = be.rhs() instanceof StringRvalExpr sre ? sre.value() : "";
              result.add(
                  new ImmediateLowerStmt(
                      new Addressable.Reg(Register.R1), new StringRvalExpr(lhs + rhs)));
              result.add(new BranchLinkLowerStmt("strcpy"));
            } else {
              if (maybeLhsLength.isPresent()) {
                final var lhs = be.lhs() instanceof StringRvalExpr sre ? sre.value() : "";
                result.add(
                    new ImmediateLowerStmt(
                        new Addressable.Reg(Register.R1), new StringRvalExpr(lhs)));
                result.add(new BranchLinkLowerStmt("strcpy"));
                final var rhs = (IdRvalExpr) be.rhs();
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R1), new Addressable.IdRval(rhs)));
                result.add(new BranchLinkLowerStmt("strcat"));
              } else if (maybeRhsLength.isPresent()) {
                final var lhs = (IdRvalExpr) be.lhs();
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R1), new Addressable.IdRval(lhs)));
                result.add(new BranchLinkLowerStmt("strcpy"));
                final var rhs = be.rhs() instanceof StringRvalExpr sre ? sre.value() : "";
                result.add(
                    new ImmediateLowerStmt(
                        new Addressable.Reg(Register.R1), new StringRvalExpr(rhs)));
                result.add(new BranchLinkLowerStmt("strcat"));
              } else {
                final var lhs = (IdRvalExpr) be.lhs();
                final var rhs = (IdRvalExpr) be.rhs();
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R1), new Addressable.IdRval(lhs)));
                result.add(new BranchLinkLowerStmt("strcpy"));
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.R1), new Addressable.IdRval(rhs)));
                result.add(new BranchLinkLowerStmt("strcat"));
              }
            }

            // move from R0 to lhs of var_assign
            result.add(
                new MovLowerStmt(new Addressable.IdRval(dest), new Addressable.Reg(Register.R0)));

            yield Collections.unmodifiableList(result);
          }
        }

        if (be.op() == BinaryOp.MULT) {
          if (be.lhs() instanceof IntRvalExpr ire
              && ire.value() >= 2
              && (ire.value() & (ire.value() - 1)) == 0) {
            // lhs is power of 2
            final var rhsIdRvalExprChunk = rvaltoIdRval(be.rhs(), gen);
            yield ImmutableList.<LowerStmt>builder()
                .addAll(rhsIdRvalExprChunk.lowerStmtList)
                .add(
                    new BitLowerStmt(
                        BitOp.LSL,
                        dest,
                        rhsIdRvalExprChunk.idRvalExpr,
                        Integer.numberOfTrailingZeros(ire.value())))
                .build();
          }
          if (be.rhs() instanceof IntRvalExpr ire
              && ire.value() >= 2
              && (ire.value() & (ire.value() - 1)) == 0) {
            // rhs is power of 2
            final var lhsIdRvalExprChunk = rvaltoIdRval(be.lhs(), gen);
            yield ImmutableList.<LowerStmt>builder()
                .addAll(lhsIdRvalExprChunk.lowerStmtList)
                .add(
                    new BitLowerStmt(
                        BitOp.LSL,
                        dest,
                        lhsIdRvalExprChunk.idRvalExpr,
                        Integer.numberOfTrailingZeros(ire.value())))
                .build();
          }
        }

        yield switch (be.op()) {
          case PLUS, AND, OR, LT, GT, LEQ, GEQ, EQ, NEQ -> {
            // can be easily flipped
            if (!(be.lhs() instanceof IdRvalExpr)) {
              final var rhsIdRvalExprChunk = rvaltoIdRval(be.rhs(), gen);
              yield ImmutableList.<LowerStmt>builder()
                  .addAll(rhsIdRvalExprChunk.lowerStmtList)
                  .add(
                      new BinaryLowerStmt(
                          be.op().lhsRhsFlip(),
                          new Addressable.IdRval(dest),
                          new Addressable.IdRval(rhsIdRvalExprChunk.idRvalExpr),
                          be.lhs()))
                  .build();
            }
            final var lhsIdRvalExprChunk = rvaltoIdRval(be.lhs(), gen);
            yield ImmutableList.<LowerStmt>builder()
                .addAll(lhsIdRvalExprChunk.lowerStmtList)
                .add(
                    new BinaryLowerStmt(
                        be.op(),
                        new Addressable.IdRval(dest),
                        new Addressable.IdRval(lhsIdRvalExprChunk.idRvalExpr),
                        be.rhs()))
                .build();
          }
          case MINUS -> {
            if (!(be.lhs() instanceof IdRvalExpr)) {
              final var rhsIdRvalExprChunk = rvaltoIdRval(be.rhs(), gen);
              yield ImmutableList.<LowerStmt>builder()
                  .addAll(rhsIdRvalExprChunk.lowerStmtList)
                  .add(
                      new ReverseSubtractLowerStmt(
                          new Addressable.IdRval(dest),
                          new Addressable.IdRval(rhsIdRvalExprChunk.idRvalExpr),
                          be.lhs()))
                  .build();
            }
            final var lhsIdRvalExprChunk = rvaltoIdRval(be.lhs(), gen);
            yield ImmutableList.<LowerStmt>builder()
                .addAll(lhsIdRvalExprChunk.lowerStmtList)
                .add(
                    new BinaryLowerStmt(
                        be.op(),
                        new Addressable.IdRval(dest),
                        new Addressable.IdRval(lhsIdRvalExprChunk.idRvalExpr),
                        be.rhs()))
                .build();
          }
          case MULT, DIV -> {
            final var lhsIdRvalExprChunk = rvaltoIdRval(be.lhs(), gen);
            final var rhsIdRvalExprChunk = rvaltoIdRval(be.rhs(), gen);
            yield ImmutableList.<LowerStmt>builder()
                .addAll(lhsIdRvalExprChunk.lowerStmtList)
                .addAll(rhsIdRvalExprChunk.lowerStmtList)
                .add(
                    new RegBinaryLowerStmt(
                        be.op(),
                        new Addressable.IdRval(dest),
                        new Addressable.IdRval(lhsIdRvalExprChunk.idRvalExpr),
                        new Addressable.IdRval(rhsIdRvalExprChunk.idRvalExpr)))
                .build();
          }
        };
      }
      case UNARY -> {
        final var ue = (UnaryExpr) expr;
        yield switch (ue.op()) {
          case NOT -> switch (ue.rval().getRvalExprType()) {
            case INT, NULL, STRING -> throw new RuntimeException();
            case BOOL -> {
              final var bre = (BoolRvalExpr) ue.rval();
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(dest), new BoolRvalExpr(!bre.value())));
            }
            case ID -> {
              final var ire = (IdRvalExpr) ue.rval();
              yield List.of(new UnaryLowerStmt(ue.op(), dest, ire));
            }
          };
          case NEGATIVE -> switch (ue.rval().getRvalExprType()) {
            case BOOL, NULL, STRING -> throw new RuntimeException();
            case INT -> {
              final var ire = (IntRvalExpr) ue.rval();
              yield List.of(
                  new ImmediateLowerStmt(
                      new Addressable.IdRval(dest), new IntRvalExpr(-ire.value())));
            }
            case ID -> {
              final var ire = (IdRvalExpr) ue.rval();
              yield List.of(new UnaryLowerStmt(ue.op(), dest, ire));
            }
          };
        };
      }
      case FIELD -> {
        final var fe = (FieldExpr) expr;
        yield List.of(new FieldAccessLowerStmt(dest, fe.target(), fe.field()));
      }
      case RVAL -> {
        final var re = (RvalExpr) expr;
        yield switch (re.getRvalExprType()) {
          case ID -> List.of(
              new MovLowerStmt(
                  new Addressable.IdRval(dest), new Addressable.IdRval((IdRvalExpr) re)));
          case STRING, INT, BOOL -> List.of(
              new ImmediateLowerStmt(new Addressable.IdRval(dest), (LiteralRvalExpr) re));
          case NULL -> {
            // Null for classes is different from for strings
            if (destType instanceof Type.KlassType) {
              yield List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(dest), new IntRvalExpr(0)));
            }
            yield List.of(
                new ImmediateLowerStmt(new Addressable.IdRval(dest), new StringRvalExpr("")));
          }
        };
      }
      case CALL -> {
        final var ce = (CallExpr) expr;
        yield ImmutableList.<LowerStmt>builder()
            .addAll(genCall(ce.args(), ce.target().id(), typeMap, cnameToData, gen))
            .add(new MovLowerStmt(new Addressable.IdRval(dest), new Addressable.Reg(Register.R0)))
            .build();
      }
      case NEW -> {
        final var ne = (NewExpr) expr;
        final var data = cnameToData.get(ne.cname());
        // Do not generate calloc(1, 0)
        if (data.sizeof() == 0) {
          yield List.of();
        }
        // Use calloc for "shallow" initialization
        yield List.of(
            new ImmediateLowerStmt(new Addressable.Reg(Register.R1), new IntRvalExpr(1)),
            new ImmediateLowerStmt(
                new Addressable.Reg(Register.R1), new IntRvalExpr(data.sizeof())),
            new BranchLinkLowerStmt("calloc"),
            new MovLowerStmt(new Addressable.IdRval(dest), new Addressable.Reg(Register.R0)));
      }
    };
  }

  public static List<LowerStmt> passReplaceLargeImmediate(
      List<LowerStmt> lowerStmtList, TempVarGen gen) {
    final var stmtList = new ArrayList<LowerStmt>();
    for (final var stmt : lowerStmtList) {
      if (stmt instanceof BinaryLowerStmt b
          && b.rhs() instanceof IntRvalExpr ire
          && !isValidOp2Imm(ire.value())) {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        final var dest = new Addressable.IdRval(idRvalExpr);
        stmtList.add(new LoadLargeImmediateLowerStmt(dest, ire));
        stmtList.add(new BinaryLowerStmt(b.op(), b.dest(), b.lhs(), idRvalExpr));
      } else if (stmt instanceof CmpLowerStmt c
          && c.rhs() instanceof IntRvalExpr ire
          && !isValidOp2Imm(ire.value())) {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        final var dest = new Addressable.IdRval(idRvalExpr);
        stmtList.add(new LoadLargeImmediateLowerStmt(dest, ire));
        stmtList.add(new CmpLowerStmt(c.op(), c.lhs(), idRvalExpr, c.dest()));
      } else if (stmt instanceof ImmediateLowerStmt i
          && i.value() instanceof IntRvalExpr ire
          && !isValidOp2Imm(ire.value())) {
        stmtList.add(new LoadLargeImmediateLowerStmt(i.dest(), ire));
      } else if (stmt instanceof ReverseSubtractLowerStmt r
          && r.rhs() instanceof IntRvalExpr ire
          && !isValidOp2Imm(ire.value())) {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        final var dest = new Addressable.IdRval(idRvalExpr);
        stmtList.add(new LoadLargeImmediateLowerStmt(dest, ire));
        stmtList.add(new ReverseSubtractLowerStmt(r.dest(), r.lhs(), idRvalExpr));
      } else {
        stmtList.add(stmt);
      }
    }
    return Collections.unmodifiableList(stmtList);
  }

  private static boolean isValidOp2Imm(int value) {
    // Assembler is able to substitute MVN/CMN/equivalent NOT for negative numbers
    final var normalisedValue = value >= 0 ? value : ~value;

    final var leading = Integer.numberOfLeadingZeros(normalisedValue);
    final var trailing = Integer.numberOfTrailingZeros(normalisedValue);
    final var length = 32 - leading - trailing;
    // Requirement:
    // any value that can be produced by rotating an 8-bit value right by any even number of bits
    // within a 32-bit word
    if (length > 8) {
      // normalisedValue is longer than 8-bit
      return false;
    }
    if (length == 8 && (trailing & 1) == 1) {
      // normalisedValue is rotated by an odd number of bits
      return false;
    }
    return true;
  }
}
