package jlitec.backend.passes.optimization.constantfolding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.Program;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.BranchLinkLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;
import jlitec.backend.passes.optimization.OptimizationPass;
import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;

public class ConstantFoldingOptimizationPass implements OptimizationPass {
  @Override
  public Program pass(Program input) {
    var program = input;
    while (true) {
      final var methodList =
          program.methodList().stream()
              .map(ConstantFoldingOptimizationPass::pass)
              .map(ConstantFoldingOptimizationPass::passInlinePrintlnBool)
              .collect(Collectors.toUnmodifiableList());
      final var newProgram = new Program(program.dataList(), methodList);
      if (newProgram.equals(program)) {
        return program;
      }
      program = newProgram;
    }
  }

  private static Method passInlinePrintlnBool(Method input) {
    final var stmtList = new ArrayList<LowerStmt>();
    for (int i = 0; i < input.lowerStmtList().size(); i++) {
      final var stmt = input.lowerStmtList().get(i);
      if (i == input.lowerStmtList().size() - 1) {
        stmtList.add(stmt);
        continue;
      }
      if (!(stmt instanceof ImmediateLowerStmt ils
          && ils.dest() instanceof Addressable.Reg r
          && r.reg().equals(Register.R0)
          && ils.value() instanceof BoolRvalExpr b)) {
        stmtList.add(stmt);
        continue;
      }
      final var nextStmt = input.lowerStmtList().get(i + 1);
      if (!(nextStmt instanceof BranchLinkLowerStmt blls && blls.target().equals("println_bool"))) {
        stmtList.add(stmt);
        continue;
      }
      // Pattern matched
      stmtList.add(
          new ImmediateLowerStmt(ils.dest(), new StringRvalExpr(b.value() ? "true" : "false")));
      stmtList.add(new BranchLinkLowerStmt("puts"));
      i++;
    }
    return new Method(
        input.returnType(),
        input.id(),
        input.argsWithThis(),
        input.vars(),
        input.spilled(),
        stmtList);
  }

  private static Method pass(Method input) {
    final var inOut = new ReachingPass().pass(input);
    final var stmtList = new ArrayList<LowerStmt>();
    for (int i = 0; i < input.lowerStmtList().size(); i++) {
      final var stmt = input.lowerStmtList().get(i);
      final var in = inOut.in().get(i);
      final List<LowerStmt> stmtChunk =
          switch (stmt.stmtExtensionType()) {
            case BRANCH_LINK, GOTO, FIELD_ASSIGN, FIELD_ACCESS, LABEL, LOAD_STACK_ARG, LDR_SPILL, PUSH_STACK, STR_SPILL, RETURN, PUSH_PAD_STACK, POP_STACK, IMMEDIATE -> List
                .of(stmt);
            case REVERSE_SUBTRACT -> {
              final var rss = (ReverseSubtractLowerStmt) stmt;
              if (!(rss.lhs() instanceof Addressable.IdRval idRvalLhs)) {
                yield List.of(stmt);
              }
              final var resolvedLhs = resolve(idRvalLhs.idRvalExpr(), in, input.lowerStmtList());
              final var resolvedRhs = resolve(rss.rhs(), in, input.lowerStmtList());
              yield switch (resolvedLhs.getRvalExprType()) {
                case NULL, STRING, BOOL -> throw new RuntimeException();
                case INT -> {
                  final var lhs = (IntRvalExpr) resolvedLhs;
                  yield switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT -> {
                      final var rhs = (IntRvalExpr) resolvedRhs;
                      yield List.of(
                          new ImmediateLowerStmt(
                              rss.dest(), new IntRvalExpr(rhs.value() - lhs.value())));
                    }
                      // Give up
                    case ID -> List.of(stmt);
                  };
                }
                  // Give up
                case ID -> List.of(stmt);
              };
            }
            case BINARY -> {
              final var bs = (BinaryLowerStmt) stmt;
              if (!(bs.lhs() instanceof Addressable.IdRval idRvalLhs)) {
                yield List.of(stmt);
              }
              final var resolvedLhs = resolve(idRvalLhs.idRvalExpr(), in, input.lowerStmtList());
              final var resolvedRhs = resolve(bs.rhs(), in, input.lowerStmtList());
              yield switch (bs.op()) {
                case AND -> switch (resolvedLhs.getRvalExprType()) {
                  case NULL, STRING, INT -> throw new RuntimeException();
                  case ID -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, INT -> throw new RuntimeException();
                    case BOOL -> {
                      final var lhs = (IdRvalExpr) resolvedLhs;
                      final var rhs = (BoolRvalExpr) resolvedRhs;
                      if (rhs.value()) {
                        yield List.of(new MovLowerStmt(bs.dest(), new Addressable.IdRval(lhs)));
                      } else {
                        yield List.of(new ImmediateLowerStmt(bs.dest(), new BoolRvalExpr(false)));
                      }
                    }
                      // Give up
                    case ID -> List.of(stmt);
                  };
                  case BOOL -> {
                    final var lhs = (BoolRvalExpr) resolvedLhs;
                    if (!lhs.value()) {
                      yield List.of(new ImmediateLowerStmt(bs.dest(), new BoolRvalExpr(false)));
                    }
                    yield switch (resolvedRhs.getRvalExprType()) {
                      case NULL, STRING, INT -> throw new RuntimeException();
                      case ID -> {
                        final var rhs = (IdRvalExpr) resolvedRhs;
                        yield List.of(new MovLowerStmt(bs.dest(), new Addressable.IdRval(rhs)));
                      }
                      case BOOL -> {
                        final var rhs = (BoolRvalExpr) resolvedRhs;
                        yield List.of(new ImmediateLowerStmt(bs.dest(), rhs));
                      }
                    };
                  }
                };
                case OR -> switch (resolvedLhs.getRvalExprType()) {
                  case NULL, STRING, INT -> throw new RuntimeException();
                  case ID -> {
                    final var lhs = (IdRvalExpr) resolvedLhs;
                    yield switch (resolvedRhs.getRvalExprType()) {
                      case NULL, STRING, INT -> throw new RuntimeException();
                      case BOOL -> {
                        final var rhs = (BoolRvalExpr) resolvedRhs;
                        if (rhs.value()) {
                          yield List.of(new ImmediateLowerStmt(bs.dest(), new BoolRvalExpr(true)));
                        } else {
                          yield List.of(new MovLowerStmt(bs.dest(), new Addressable.IdRval(lhs)));
                        }
                      }
                        // Give up
                      case ID -> List.of(stmt);
                    };
                  }
                  case BOOL -> {
                    final var lhs = (BoolRvalExpr) resolvedLhs;
                    if (lhs.value()) {
                      yield List.of(new ImmediateLowerStmt(bs.dest(), new BoolRvalExpr(true)));
                    }
                    yield switch (resolvedRhs.getRvalExprType()) {
                      case NULL, STRING, INT -> throw new RuntimeException();
                      case ID -> {
                        final var rhs = (IdRvalExpr) resolvedRhs;
                        yield List.of(new MovLowerStmt(bs.dest(), new Addressable.IdRval(rhs)));
                      }
                      case BOOL -> {
                        final var rhs = (BoolRvalExpr) resolvedRhs;
                        yield List.of(new ImmediateLowerStmt(bs.dest(), rhs));
                      }
                    };
                  }
                };
                case PLUS, MINUS, MULT, DIV, LT, GT, LEQ, GEQ, EQ, NEQ -> switch (resolvedLhs
                    .getRvalExprType()) {
                  case NULL, STRING, BOOL -> throw new RuntimeException();
                  case INT -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT -> genStmtChunkBinaryInt(
                        (IntRvalExpr) resolvedLhs, (IntRvalExpr) resolvedRhs, bs.dest(), bs.op());
                      // Give up
                    case ID -> List.of(stmt);
                  };
                    // Give up
                  case ID -> List.of(stmt);
                };
              };
            }
            case BIT -> {
              final var bs = (BitLowerStmt) stmt;
              final var resolvedExpr = resolve(bs.expr(), in, input.lowerStmtList());
              yield switch (resolvedExpr.getRvalExprType()) {
                case NULL, STRING, BOOL -> throw new RuntimeException();
                case INT -> {
                  final var expr = (IntRvalExpr) resolvedExpr;
                  final var value =
                      switch (bs.op()) {
                        case ROR -> Integer.rotateRight(expr.value(), bs.shift());
                        case ASR -> expr.value() >> bs.shift();
                        case LSR -> expr.value() >>> bs.shift();
                        case LSL -> expr.value() << bs.shift();
                      };
                  yield List.of(
                      new ImmediateLowerStmt(
                          new Addressable.IdRval(bs.dest()), new IntRvalExpr(value)));
                }
                  // Give up
                case ID -> List.of(stmt);
              };
            }
            case CMP -> {
              final var cs = (CmpLowerStmt) stmt;
              final var resolvedLhs = resolve(cs.lhs(), in, input.lowerStmtList());
              final var resolvedRhs = resolve(cs.rhs(), in, input.lowerStmtList());
              yield switch (cs.op()) {
                  // can be bool or int
                case EQ -> switch (resolvedLhs.getRvalExprType()) {
                  case NULL, STRING -> throw new RuntimeException();
                  case BOOL -> {
                    // Guaranteed to have a boolean RHS
                    final var lhs = (BoolRvalExpr) resolvedLhs;
                    final var rhs = (BoolRvalExpr) cs.rhs();
                    if (lhs.value() == rhs.value()) {
                      yield List.of(new GotoLowerStmt(cs.dest()));
                    } else {
                      yield List.of();
                    }
                  }
                  case INT -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT -> genStmtChunkCmpInt(
                        (IntRvalExpr) resolvedLhs, (IntRvalExpr) resolvedRhs, cs.dest(), cs.op());
                      // Give up
                    case ID -> List.of(stmt);
                  };
                    // Give up
                  case ID -> List.of(stmt);
                };
                case AND -> switch (resolvedLhs.getRvalExprType()) {
                  case NULL, STRING, INT -> throw new RuntimeException();
                  case BOOL -> {
                    final var lhs = (BoolRvalExpr) resolvedLhs;
                    if (!lhs.value()) {
                      yield List.of();
                    }
                    yield switch (resolvedRhs.getRvalExprType()) {
                      case NULL, STRING, INT -> throw new RuntimeException();
                      case BOOL -> {
                        final var rhs = (BoolRvalExpr) resolvedRhs;
                        yield rhs.value() ? List.of(new GotoLowerStmt(cs.dest())) : List.of();
                      }
                      case ID -> List.of(
                          new CmpLowerStmt(
                              BinaryOp.EQ,
                              (IdRvalExpr) resolvedRhs,
                              new BoolRvalExpr(true),
                              cs.dest()));
                    };
                  }
                  case ID -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, INT -> throw new RuntimeException();
                    case BOOL -> {
                      final var rhs = (BoolRvalExpr) resolvedLhs;
                      if (!rhs.value()) {
                        yield List.of();
                      }
                      yield List.of(
                          new CmpLowerStmt(
                              BinaryOp.EQ, cs.lhs(), new BoolRvalExpr(true), cs.dest()));
                    }
                      // Give up
                    case ID -> List.of(stmt);
                  };
                };
                case OR -> switch (resolvedLhs.getRvalExprType()) {
                  case NULL, STRING, INT -> throw new RuntimeException();
                  case BOOL -> {
                    final var lhs = (BoolRvalExpr) resolvedLhs;
                    if (lhs.value()) {
                      yield List.of(new GotoLowerStmt(cs.dest()));
                    }
                    yield switch (resolvedRhs.getRvalExprType()) {
                      case NULL, STRING, INT -> throw new RuntimeException();
                      case BOOL -> {
                        final var rhs = (BoolRvalExpr) resolvedRhs;
                        yield rhs.value() ? List.of(new GotoLowerStmt(cs.dest())) : List.of();
                      }
                      case ID -> List.of(
                          new CmpLowerStmt(
                              BinaryOp.EQ,
                              (IdRvalExpr) resolvedRhs,
                              new BoolRvalExpr(true),
                              cs.dest()));
                    };
                  }
                  case ID -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, INT -> throw new RuntimeException();
                    case BOOL -> {
                      final var rhs = (BoolRvalExpr) resolvedLhs;
                      if (rhs.value()) {
                        yield List.of(new GotoLowerStmt(cs.dest()));
                      }
                      yield List.of(
                          new CmpLowerStmt(
                              BinaryOp.EQ, cs.lhs(), new BoolRvalExpr(true), cs.dest()));
                    }
                      // Give up
                    case ID -> List.of(stmt);
                  };
                };
                case GT, LT, GEQ, LEQ, NEQ -> switch (resolvedLhs.getRvalExprType()) {
                  case NULL, STRING, BOOL -> throw new RuntimeException();
                  case INT -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT -> genStmtChunkCmpInt(
                        (IntRvalExpr) resolvedLhs, (IntRvalExpr) resolvedRhs, cs.dest(), cs.op());
                      // Give up
                    case ID -> List.of(stmt);
                  };
                    // Give up
                  case ID -> List.of(stmt);
                };
                case PLUS, MINUS, MULT, DIV -> throw new RuntimeException("should not be reached");
              };
            }
            case MOV -> {
              final var ms = (MovLowerStmt) stmt;
              if (!(ms.src() instanceof Addressable.IdRval srcIdRval)) {
                yield List.of(stmt);
              }
              final var resolved = resolve(srcIdRval.idRvalExpr(), in, input.lowerStmtList());
              yield switch (resolved.getRvalExprType()) {
                case NULL -> List.of(new ImmediateLowerStmt(ms.dst(), new StringRvalExpr("")));
                case STRING, INT, BOOL -> List.of(new ImmediateLowerStmt(ms.dst(), resolved));
                case ID -> List.of(stmt);
              };
            }
            case REG_BINARY -> {
              final var rb = (RegBinaryLowerStmt) stmt;
              if (!(rb.lhs() instanceof Addressable.IdRval lhsIdRval
                  && rb.rhs() instanceof Addressable.IdRval rhsIdRval)) {
                yield List.of(stmt);
              }
              final var resolvedLhs = resolve(lhsIdRval.idRvalExpr(), in, input.lowerStmtList());
              final var resolvedRhs = resolve(rhsIdRval.idRvalExpr(), in, input.lowerStmtList());
              yield switch (rb.op()) {
                case AND, OR -> throw new RuntimeException();
                case PLUS, MINUS, MULT, DIV, LT, GT, LEQ, GEQ, EQ, NEQ -> switch (resolvedLhs
                    .getRvalExprType()) {
                  case NULL, STRING, BOOL -> throw new RuntimeException();
                  case INT -> switch (resolvedRhs.getRvalExprType()) {
                    case NULL, STRING, BOOL -> throw new RuntimeException();
                    case INT -> genStmtChunkBinaryInt(
                        (IntRvalExpr) resolvedLhs, (IntRvalExpr) resolvedRhs, rb.dest(), rb.op());
                      // Give up
                    case ID -> List.of(stmt);
                  };
                    // Give up
                  case ID -> List.of(stmt);
                };
              };
            }
            case UNARY -> {
              final var us = (UnaryLowerStmt) stmt;
              final var resolved = resolve(us.expr(), in, input.lowerStmtList());
              yield switch (us.op()) {
                case NOT -> switch (resolved.getRvalExprType()) {
                  case NULL, STRING, INT -> throw new RuntimeException();
                  case BOOL -> List.of(
                      new ImmediateLowerStmt(new Addressable.IdRval(us.dest()), resolved));
                    // Give up
                  case ID -> List.of(stmt);
                };
                case NEGATIVE -> switch (resolved.getRvalExprType()) {
                  case NULL, STRING, BOOL -> throw new RuntimeException();
                  case INT -> List.of(
                      new ImmediateLowerStmt(new Addressable.IdRval(us.dest()), resolved));
                    // Give up
                  case ID -> List.of(stmt);
                };
              };
            }
          };
      stmtList.addAll(stmtChunk);
    }
    return new Method(
        input.returnType(),
        input.id(),
        input.argsWithThis(),
        input.vars(),
        input.spilled(),
        stmtList);
  }

  private static List<LowerStmt> genStmtChunkBinaryInt(
      IntRvalExpr lhsRvalExpr, IntRvalExpr rhsRvalExpr, Addressable dest, BinaryOp op) {
    final var lhs = lhsRvalExpr.value();
    final var rhs = rhsRvalExpr.value();
    return switch (op) {
      case AND, OR -> throw new RuntimeException();
      case LT -> List.of(new ImmediateLowerStmt(dest, new BoolRvalExpr(lhs < rhs)));
      case GT -> List.of(new ImmediateLowerStmt(dest, new BoolRvalExpr(lhs > rhs)));
      case LEQ -> List.of(new ImmediateLowerStmt(dest, new BoolRvalExpr(lhs <= rhs)));
      case GEQ -> List.of(new ImmediateLowerStmt(dest, new BoolRvalExpr(lhs >= rhs)));
      case EQ -> List.of(new ImmediateLowerStmt(dest, new BoolRvalExpr(lhs == rhs)));
      case NEQ -> List.of(new ImmediateLowerStmt(dest, new BoolRvalExpr(lhs != rhs)));
      case MULT -> List.of(new ImmediateLowerStmt(dest, new IntRvalExpr(lhs * rhs)));
      case DIV -> List.of(new ImmediateLowerStmt(dest, new IntRvalExpr(lhs / rhs)));
      case PLUS -> List.of(new ImmediateLowerStmt(dest, new IntRvalExpr(lhs + rhs)));
      case MINUS -> List.of(new ImmediateLowerStmt(dest, new IntRvalExpr(lhs - rhs)));
    };
  }

  private static List<LowerStmt> genStmtChunkCmpInt(
      IntRvalExpr lhs, IntRvalExpr rhs, String dest, BinaryOp op) {
    return switch (op) {
      case LT -> {
        if (lhs.value() < rhs.value()) {
          yield List.of(new GotoLowerStmt(dest));
        } else {
          yield List.of();
        }
      }
      case GT -> {
        if (lhs.value() > rhs.value()) {
          yield List.of(new GotoLowerStmt(dest));
        } else {
          yield List.of();
        }
      }
      case LEQ -> {
        if (lhs.value() <= rhs.value()) {
          yield List.of(new GotoLowerStmt(dest));
        } else {
          yield List.of();
        }
      }
      case GEQ -> {
        if (lhs.value() >= rhs.value()) {
          yield List.of(new GotoLowerStmt(dest));
        } else {
          yield List.of();
        }
      }
      case EQ -> {
        if (lhs.value() == rhs.value()) {
          yield List.of(new GotoLowerStmt(dest));
        } else {
          yield List.of();
        }
      }
      case NEQ -> {
        if (lhs.value() != rhs.value()) {
          yield List.of(new GotoLowerStmt(dest));
        } else {
          yield List.of();
        }
      }
      case AND, OR, PLUS, MINUS, MULT, DIV -> throw new RuntimeException("should not be reached");
    };
  }

  private static RvalExpr resolve(RvalExpr expr, Set<Integer> in, List<LowerStmt> stmtList) {
    return switch (expr.getRvalExprType()) {
      case ID -> {
        final var ire = (IdRvalExpr) expr;
        final var maybeResult = getRvalExpr(new Node.Id(ire), in, stmtList);
        yield maybeResult.orElse(expr);
      }
      case BOOL, INT, NULL, STRING -> expr;
    };
  }

  private static Optional<RvalExpr> getRvalExpr(
      Node node, Set<Integer> in, List<LowerStmt> stmtList) {
    final var definesConstant =
        in.stream()
            .filter(idx -> ReachingPass.nodesOf(stmtList.get(idx)).contains(node))
            .collect(Collectors.toUnmodifiableList());
    if (definesConstant.size() != 1) {
      return Optional.empty();
    }
    final var definingStmt = stmtList.get(definesConstant.get(0));
    return getRvalExpr(definingStmt);
  }

  private static Optional<RvalExpr> getRvalExpr(LowerStmt definingStmt) {
    return switch (definingStmt.stmtExtensionType()) {
      case IMMEDIATE -> {
        final var is = (ImmediateLowerStmt) definingStmt;
        yield Optional.of(is.value());
      }
      case BINARY, BIT, REG_BINARY, UNARY, LABEL, BRANCH_LINK, CMP, FIELD_ASSIGN, GOTO, LOAD_STACK_ARG, FIELD_ACCESS, LDR_SPILL, STR_SPILL, RETURN, MOV, PUSH_PAD_STACK, PUSH_STACK, POP_STACK, REVERSE_SUBTRACT -> Optional
          .empty();
    };
  }
}
