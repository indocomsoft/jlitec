package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record UnaryExpr(UnaryOp op, Expr expr, Location leftLocation, Location rightLocation)
    implements Expr {
  /** Constructor that checks the type validity. */
  public UnaryExpr {
    // Force this line to be an expression such that exhaustiveness is enforced.
    op =
        switch (op) {
          case NOT -> {
            if (expr.getTypeHint().filter(t -> t != TypeHint.BOOL).isPresent()) {
              throw new IncompatibleTypeException(op, expr, TypeHint.BOOL);
            }
            yield op;
          }
          case NEGATIVE -> {
            if (expr.getTypeHint().filter(t -> t != TypeHint.INT).isPresent()) {
              throw new IncompatibleTypeException(op, expr, TypeHint.INT);
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
  public Optional<TypeHint> getTypeHint() {
    return switch (op) {
      case NOT -> Optional.of(TypeHint.BOOL);
      case NEGATIVE -> Optional.of(TypeHint.INT);
    };
  }

  @Override
  public String print(int indent) {
    return op.toString() + expr.print(indent);
  }
}
