package jlitec.ast.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record IdExpr(String id, Location leftLocation, Location rightLocation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_ID;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return id;
  }
}
