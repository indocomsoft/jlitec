package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;
import jlitec.Printable;

public record ParenExpr(Expr expr, Location leftLocation, Location rightLocation)
    implements Expr, Printable {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_PAREN;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return "(" + expr.print(indent) + ")";
  }
}
