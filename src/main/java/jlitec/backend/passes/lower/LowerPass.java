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
import jlitec.backend.passes.lower.stmt.BooleanNeqLowerStmt;
import jlitec.backend.passes.lower.stmt.BranchLinkLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAssignLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LabelLowerStmt;
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
import jlitec.ir3.expr.FieldExpr;
import jlitec.ir3.expr.NewExpr;
import jlitec.ir3.expr.UnaryExpr;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
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

    final var vars =
        Stream.concat(method.vars().stream(), gen.getVars().stream())
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(), method.id().replace("%", ""), argsWithThis, vars, List.of(), stmtList);
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
                    case ID -> throw new RuntimeException("should not be reached");
                    case NULL, STRING -> gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
                    case INT -> gen.gen(new Type.PrimitiveType(Ir3Type.INT));
                    case BOOL -> gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
                  };
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), be.lhs()),
                  new CmpLowerStmt(be.op(), idRvalExpr, be.rhs(), cs.dest().label()));
            }
          }
          case RVAL -> {
            final var re = (RvalExpr) condition;
            final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
            final var idRvalExpr = new IdRvalExpr(tempVar.id());
            yield switch (re.getRvalExprType()) {
              case ID -> {
                final var ire = (IdRvalExpr) re;
                yield List.of(
                    new MovLowerStmt(
                        new Addressable.IdRval(idRvalExpr), new Addressable.IdRval(ire)),
                    new CmpLowerStmt(
                        BinaryOp.EQ, idRvalExpr, new BoolRvalExpr(true), cs.dest().label()));
              }
              case STRING, NULL, INT, BOOL -> List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), re),
                  new CmpLowerStmt(
                      BinaryOp.EQ, idRvalExpr, new BoolRvalExpr(true), cs.dest().label()));
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
          case INT, BOOL -> List.of(
              new BranchLinkLowerStmt("readln_int_bool"),
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
              new ImmediateLowerStmt(new Addressable.Reg(Register.R0), ps.rval()),
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
        yield switch (vas.rhs().getExprType()) {
          case BINARY -> {
            final var be = (BinaryExpr) vas.rhs();

            if (be.op() == BinaryOp.PLUS) {
              final var isLhsString =
                  switch (be.lhs().getRvalExprType()) {
                    case BOOL, INT -> false;
                    case STRING, NULL -> true;
                    case ID -> typeMap.get(((IdRvalExpr) be.lhs()).id()).type() == Ir3Type.STRING;
                  };
              if (isLhsString) {
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
                          new Addressable.Reg(Register.R0),
                          new IntRvalExpr(maybeTotalLength.get())));
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
                    throw new RuntimeException("should not be reached");
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
                    throw new RuntimeException("should not be reached");
                  }
                }

                // move from R0 to lhs of var_assign
                result.add(
                    new MovLowerStmt(
                        new Addressable.IdRval(vas.lhs()), new Addressable.Reg(Register.R0)));

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
                            vas.lhs(),
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
                            vas.lhs(),
                            lhsIdRvalExprChunk.idRvalExpr,
                            Integer.numberOfTrailingZeros(ire.value())))
                    .build();
              }
            }

            yield switch (be.op()) {
              case NEQ -> switch (be.lhs().getRvalExprType()) {
                case NULL, STRING, BOOL -> throw new RuntimeException();
                case INT -> {
                  final var lhs = (IntRvalExpr) be.lhs();
                  yield switch (be.rhs().getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT -> {
                      final var rhs = (IntRvalExpr) be.rhs();
                      // Wheee constant folding
                      yield List.of(
                          new ImmediateLowerStmt(
                              new Addressable.IdRval(vas.lhs()),
                              new BoolRvalExpr(lhs.value() != rhs.value())));
                    }
                    case ID -> {
                      final var rhs = (IdRvalExpr) be.rhs();
                      yield List.of(new BooleanNeqLowerStmt(vas.lhs(), rhs, lhs));
                    }
                  };
                }
                case ID -> {
                  final var lhs = (IdRvalExpr) be.lhs();
                  yield switch (be.rhs().getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT, ID -> {
                      final var rhs = (IdRvalExpr) be.rhs();
                      yield List.of(new BooleanNeqLowerStmt(vas.lhs(), lhs, rhs));
                    }
                  };
                }
              };
              case PLUS, AND, OR, LT, GT, LEQ, GEQ, EQ -> {
                // can be easily flipped
                if (!(be.lhs() instanceof IdRvalExpr)) {
                  final var rhsIdRvalExprChunk = rvaltoIdRval(be.rhs(), gen);
                  yield ImmutableList.<LowerStmt>builder()
                      .addAll(rhsIdRvalExprChunk.lowerStmtList)
                      .add(
                          new BinaryLowerStmt(
                              be.op().lhsRhsFlip(),
                              new Addressable.IdRval(vas.lhs()),
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
                            new Addressable.IdRval(vas.lhs()),
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
                              new Addressable.IdRval(vas.lhs()),
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
                            new Addressable.IdRval(vas.lhs()),
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
                            new Addressable.IdRval(vas.lhs()),
                            new Addressable.IdRval(lhsIdRvalExprChunk.idRvalExpr),
                            new Addressable.IdRval(rhsIdRvalExprChunk.idRvalExpr)))
                    .build();
              }
            };
          }
          case UNARY -> {
            final var ue = (UnaryExpr) vas.rhs();
            yield switch (ue.op()) {
              case NOT -> switch (ue.rval().getRvalExprType()) {
                case BOOL -> {
                  final var bre = (BoolRvalExpr) ue.rval();
                  yield List.of(
                      new ImmediateLowerStmt(
                          new Addressable.IdRval(vas.lhs()), new BoolRvalExpr(!bre.value())));
                }
                case INT, ID, NULL, STRING -> {
                  final var idRvalExprChunk = rvaltoIdRval(ue.rval(), gen);
                  yield ImmutableList.<LowerStmt>builder()
                      .addAll(idRvalExprChunk.lowerStmtList)
                      .add(new UnaryLowerStmt(ue.op(), vas.lhs(), idRvalExprChunk.idRvalExpr))
                      .build();
                }
              };
              case NEGATIVE -> switch (ue.rval().getRvalExprType()) {
                case INT -> {
                  final var ire = (IntRvalExpr) ue.rval();
                  yield List.of(
                      new ImmediateLowerStmt(
                          new Addressable.IdRval(vas.lhs()), new IntRvalExpr(-ire.value())));
                }
                case BOOL, ID, NULL, STRING -> {
                  final var idRvalExprChunk = rvaltoIdRval(ue.rval(), gen);
                  yield ImmutableList.<LowerStmt>builder()
                      .addAll(idRvalExprChunk.lowerStmtList)
                      .add(new UnaryLowerStmt(ue.op(), vas.lhs(), idRvalExprChunk.idRvalExpr))
                      .build();
                }
              };
            };
          }
          case FIELD -> {
            final var fe = (FieldExpr) vas.rhs();
            yield List.of(new FieldAccessLowerStmt(vas.lhs(), fe.target(), fe.field()));
          }
          case RVAL -> {
            final var re = (RvalExpr) vas.rhs();
            yield switch (re.getRvalExprType()) {
              case ID -> List.of(
                  new MovLowerStmt(
                      new Addressable.IdRval(vas.lhs()), new Addressable.IdRval((IdRvalExpr) re)));
              case STRING, NULL, INT, BOOL -> List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(vas.lhs()), re));
            };
          }
          case CALL -> {
            final var ce = (CallExpr) vas.rhs();
            final var result = new ArrayList<LowerStmt>();
            for (int i = 0; i < 4 && i < ce.args().size(); i++) {
              final var arg = ce.args().get(i);
              if (arg.getRvalExprType() == RvalExprType.ID) {
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.fromInt(i)),
                        new Addressable.IdRval((IdRvalExpr) arg)));
              } else {
                result.add(new ImmediateLowerStmt(new Addressable.Reg(Register.fromInt(i)), arg));
              }
            }
            final List<RvalExpr> stackArgs =
                ce.args().size() > 4 ? ce.args().subList(4, ce.args().size()) : List.of();
            if (!stackArgs.isEmpty()) {
              result.addAll(generatePushStackLowerStmt(stackArgs, gen, typeMap));
            }
            result.add(new BranchLinkLowerStmt(ce.target().id()));
            if (!stackArgs.isEmpty()) {
              result.add(
                  new PopStackLowerStmt(stackArgs.size() + ((stackArgs.size() & 1) == 1 ? 1 : 0)));
            }
            result.add(
                new MovLowerStmt(
                    new Addressable.IdRval(vas.lhs()), new Addressable.Reg(Register.R0)));
            yield Collections.unmodifiableList(result);
          }
          case NEW -> {
            final var ne = (NewExpr) vas.rhs();
            final var data = cnameToData.get(ne.cname());
            yield List.of(
                new ImmediateLowerStmt(
                    new Addressable.Reg(Register.R0), new IntRvalExpr(data.sizeof())),
                new BranchLinkLowerStmt("malloc"),
                new MovLowerStmt(
                    new Addressable.IdRval(vas.lhs()), new Addressable.Reg(Register.R0)));
          }
        };
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
        yield switch (fas.rhs().getExprType()) {
          case BINARY -> {
            final var be = (BinaryExpr) fas.rhs();

            if (be.op() == BinaryOp.PLUS) {
              final var isLhsString =
                  switch (be.lhs().getRvalExprType()) {
                    case BOOL, INT -> false;
                    case STRING, NULL -> true;
                    case ID -> typeMap.get(((IdRvalExpr) be.lhs()).id()).type() == Ir3Type.STRING;
                  };
              if (isLhsString) {
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
                          new Addressable.Reg(Register.R0),
                          new IntRvalExpr(maybeTotalLength.get())));
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
                    throw new RuntimeException("should not be reached");
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
                    result.add(new ImmediateLowerStmt(new Addressable.Reg(Register.R1), rhs));
                    result.add(new BranchLinkLowerStmt("strcat"));
                  } else if (maybeRhsLength.isPresent()) {
                    final var lhs = (IdRvalExpr) be.lhs();
                    result.add(new ImmediateLowerStmt(new Addressable.Reg(Register.R1), lhs));
                    result.add(new BranchLinkLowerStmt("strcpy"));
                    final var rhs = be.rhs() instanceof StringRvalExpr sre ? sre.value() : "";
                    result.add(
                        new ImmediateLowerStmt(
                            new Addressable.Reg(Register.R1), new StringRvalExpr(rhs)));
                    result.add(new BranchLinkLowerStmt("strcat"));
                  } else {
                    throw new RuntimeException("should not be reached");
                  }
                }

                // store R0 to field
                result.add(
                    new FieldAssignLowerStmt(
                        fas.lhsId(), fas.lhsField(), new Addressable.Reg(Register.R0)));

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
                            idRvalExpr,
                            rhsIdRvalExprChunk.idRvalExpr,
                            Integer.numberOfTrailingZeros(ire.value())))
                    .add(
                        new FieldAssignLowerStmt(
                            fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
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
                            idRvalExpr,
                            lhsIdRvalExprChunk.idRvalExpr,
                            Integer.numberOfTrailingZeros(ire.value())))
                    .add(
                        new FieldAssignLowerStmt(
                            fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
                    .build();
              }
            }

            final var lhsIdRvalExprChunk = rvaltoIdRval(be.lhs(), gen);

            yield switch (be.op()) {
              case LT, GT, LEQ, GEQ, EQ, NEQ, OR, AND, PLUS, MINUS -> ImmutableList
                  .<LowerStmt>builder()
                  .addAll(lhsIdRvalExprChunk.lowerStmtList)
                  .add(
                      new BinaryLowerStmt(
                          be.op(),
                          new Addressable.IdRval(idRvalExpr),
                          new Addressable.IdRval(lhsIdRvalExprChunk.idRvalExpr),
                          be.rhs()))
                  .add(
                      new FieldAssignLowerStmt(
                          fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
                  .build();
              case MULT, DIV -> {
                final var rhsIdRvalExprChunk = rvaltoIdRval(be.rhs(), gen);
                yield ImmutableList.<LowerStmt>builder()
                    .addAll(lhsIdRvalExprChunk.lowerStmtList)
                    .addAll(rhsIdRvalExprChunk.lowerStmtList)
                    .add(
                        new RegBinaryLowerStmt(
                            be.op(),
                            new Addressable.IdRval(idRvalExpr),
                            new Addressable.IdRval(lhsIdRvalExprChunk.idRvalExpr),
                            new Addressable.IdRval(rhsIdRvalExprChunk.idRvalExpr)))
                    .add(
                        new FieldAssignLowerStmt(
                            fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
                    .build();
              }
            };
          }
          case UNARY -> {
            final var ue = (UnaryExpr) fas.rhs();

            yield switch (ue.op()) {
              case NOT -> switch (ue.rval().getRvalExprType()) {
                case BOOL -> {
                  final var bre = (BoolRvalExpr) ue.rval();
                  yield List.of(
                      new ImmediateLowerStmt(
                          new Addressable.IdRval(idRvalExpr), new BoolRvalExpr(!bre.value())),
                      new FieldAssignLowerStmt(
                          fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)));
                }
                case INT, ID, NULL, STRING -> {
                  final var idRvalExprChunk = rvaltoIdRval(ue.rval(), gen);
                  yield ImmutableList.<LowerStmt>builder()
                      .addAll(idRvalExprChunk.lowerStmtList)
                      .add(new UnaryLowerStmt(ue.op(), idRvalExpr, idRvalExprChunk.idRvalExpr))
                      .add(
                          new FieldAssignLowerStmt(
                              fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
                      .build();
                }
              };
              case NEGATIVE -> switch (ue.rval().getRvalExprType()) {
                case INT -> {
                  final var ire = (IntRvalExpr) ue.rval();
                  yield List.of(
                      new ImmediateLowerStmt(
                          new Addressable.IdRval(idRvalExpr), new IntRvalExpr(-ire.value())),
                      new FieldAssignLowerStmt(
                          fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)));
                }
                case BOOL, ID, NULL, STRING -> {
                  final var idRvalExprChunk = rvaltoIdRval(ue.rval(), gen);
                  yield ImmutableList.<LowerStmt>builder()
                      .addAll(idRvalExprChunk.lowerStmtList)
                      .add(new UnaryLowerStmt(ue.op(), idRvalExpr, idRvalExprChunk.idRvalExpr))
                      .add(
                          new FieldAssignLowerStmt(
                              fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)))
                      .build();
                }
              };
            };
          }
          case FIELD -> {
            final var fe = (FieldExpr) fas.rhs();
            yield List.of(
                new FieldAccessLowerStmt(idRvalExpr, fe.target(), fe.field()),
                new FieldAssignLowerStmt(
                    fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)));
          }
          case RVAL -> {
            final var re = (RvalExpr) fas.rhs();
            yield switch (re.getRvalExprType()) {
              case ID -> List.of(
                  new FieldAssignLowerStmt(
                      fas.lhsId(), fas.lhsField(), new Addressable.IdRval((IdRvalExpr) re)));
              case STRING, NULL, INT, BOOL -> List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), re),
                  new FieldAssignLowerStmt(
                      fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)));
            };
          }
          case CALL -> {
            final var ce = (CallExpr) fas.rhs();
            final var result = new ArrayList<LowerStmt>();
            for (int i = 0; i < 4 && i < ce.args().size(); i++) {
              final var arg = ce.args().get(i);
              if (arg.getRvalExprType() == RvalExprType.ID) {
                result.add(
                    new MovLowerStmt(
                        new Addressable.Reg(Register.fromInt(i)),
                        new Addressable.IdRval((IdRvalExpr) arg)));
              } else {
                result.add(new ImmediateLowerStmt(new Addressable.Reg(Register.fromInt(i)), arg));
              }
            }
            final List<RvalExpr> stackArgs =
                ce.args().size() > 4 ? ce.args().subList(4, ce.args().size()) : List.of();
            if (!stackArgs.isEmpty()) {
              result.addAll(generatePushStackLowerStmt(stackArgs, gen, typeMap));
            }
            result.add(new BranchLinkLowerStmt(ce.target().id()));
            if (!stackArgs.isEmpty()) {
              result.add(
                  new PopStackLowerStmt(stackArgs.size() + ((stackArgs.size() & 1) == 1 ? 1 : 0)));
            }
            result.add(
                new MovLowerStmt(
                    new Addressable.IdRval(idRvalExpr), new Addressable.Reg(Register.R0)));
            result.add(
                new FieldAssignLowerStmt(
                    fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)));
            yield Collections.unmodifiableList(result);
          }
          case NEW -> {
            final var ne = (NewExpr) fas.rhs();
            final var data = cnameToData.get(ne.cname());
            yield List.of(
                new ImmediateLowerStmt(
                    new Addressable.Reg(Register.R0), new IntRvalExpr(data.sizeof())),
                new BranchLinkLowerStmt("malloc"),
                new MovLowerStmt(
                    new Addressable.IdRval(idRvalExpr), new Addressable.Reg(Register.R0)),
                new FieldAssignLowerStmt(
                    fas.lhsId(), fas.lhsField(), new Addressable.IdRval(idRvalExpr)));
          }
        };
      }
      case CALL -> {
        final var cs = (CallStmt) stmt;
        final var result = new ArrayList<LowerStmt>();
        for (int i = 0; i < 4 && i < cs.args().size(); i++) {
          final var arg = cs.args().get(i);
          if (arg.getRvalExprType() == RvalExprType.ID) {
            result.add(
                new MovLowerStmt(
                    new Addressable.Reg(Register.fromInt(i)),
                    new Addressable.IdRval((IdRvalExpr) arg)));
          } else {
            result.add(new ImmediateLowerStmt(new Addressable.Reg(Register.fromInt(i)), arg));
          }
        }
        final List<RvalExpr> stackArgs =
            cs.args().size() > 4 ? cs.args().subList(4, cs.args().size()) : List.of();
        if (!stackArgs.isEmpty()) {
          result.addAll(generatePushStackLowerStmt(stackArgs, gen, typeMap));
        }
        result.add(new BranchLinkLowerStmt(cs.target().id()));
        if (!stackArgs.isEmpty()) {
          result.add(
              new PopStackLowerStmt(stackArgs.size() + ((stackArgs.size() & 1) == 1 ? 1 : 0)));
        }
        yield Collections.unmodifiableList(result);
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
                  new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), rvalExpr),
                  new PushStackLowerStmt(idRvalExpr));
            }
            case INT -> {
              final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), rvalExpr),
                  new PushStackLowerStmt(idRvalExpr));
            }
            case STRING -> {
              final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
              final var idRvalExpr = new IdRvalExpr(tempVar.id());
              yield List.of(
                  new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), rvalExpr),
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
            List.of(new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), rvalExpr)));
      }
      case INT -> {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalExprChunk(
            idRvalExpr,
            List.of(new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), rvalExpr)));
      }
      case STRING -> {
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalExprChunk(
            idRvalExpr,
            List.of(new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), rvalExpr)));
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
}
