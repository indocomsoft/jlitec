package jlitec.ast.expr;

import java.util.Optional;

public record UnaryExpr(UnaryOp op, Expr expr) implements Expr {
  /** Constructor that checks the type validity. */
  public UnaryExpr {
    // Force this line to be an expression such that exhaustiveness is enforced.
    this.op = switch (op) {
      case NOT -> {
        if (expr.getType().filter(t -> t != Type.BOOL).isPresent()) {
          throw new IncompatibleTypeException(op, expr, Type.BOOL);
        }
        yield op;
      }
      case NEGATIVE -> {
        if (expr.getType().filter(t -> t != Type.INT).isPresent()) {
          throw new IncompatibleTypeException(op, expr, Type.INT);
        }
        yield op;
      }
    };
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_UNARY;
  }

  @Override
  public Optional<Type> getType() {
    return switch (op) {
      case NOT -> Optional.of(Type.BOOL);
      case NEGATIVE -> Optional.of(Type.INT);
    };
  }

  @Override
  public String print(int indent) {
    return op.toString() + expr.print(indent);
  }
}
