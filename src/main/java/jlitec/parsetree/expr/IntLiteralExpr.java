package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record IntLiteralExpr(int value, Location leftLocation, Location rightLocation)
    implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_INT_LITERAL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.of(TypeHint.INT);
  }

  @Override
  public String print(int indent) {
    return Integer.toString(value);
  }
}
