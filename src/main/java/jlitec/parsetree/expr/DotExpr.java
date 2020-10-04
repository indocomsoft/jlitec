package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record DotExpr(Expr target, String id, Location leftLocation, Location rightLocation)
    implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return target.print(indent) + '.' + id;
  }
}
