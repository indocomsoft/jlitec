package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record ThisExpr(Location leftLocation, Location rightLocation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_THIS;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return "this";
  }
}
