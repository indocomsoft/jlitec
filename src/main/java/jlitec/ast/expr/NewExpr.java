package jlitec.ast.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record NewExpr(String cname, Location leftLocation, Location rightLocation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NEW;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return "new " + cname + "()";
  }
}
