package jlitec.ast.expr;

import java.util.Optional;

public record UnaryExpr(UnaryOp op, Expr expr) implements Expr {
  /** Constructor that checks the type validity. */
  public UnaryExpr {
    switch (op) {
      case NOT -> {
        if (expr.getType().filter(t -> t != Type.BOOL).isPresent()) {
          throw new IncompatibleTypeException(op, expr);
        }
      }
      case NEGATIVE -> {
        if (expr.getType().filter(t -> t != Type.INT).isPresent()) {
          throw new IncompatibleTypeException(op, expr);
        }
      }
    }
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
    return new StringBuilder().append(op.toString()).append(expr.print(indent)).toString();
  }
}
