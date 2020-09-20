package jlitec.ast.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record StringLiteralExpr(String value, Location leftLocation, Location rightLocation)
    implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.of(TypeHint.STRING);
  }

  @Override
  public String print(int indent) {
    return '"' + value + '"';
  }
}
