package jlitec.ast.expr;

import java.util.Optional;
import jlitec.ast.Printable;

public record ParenExpr(Expr expr) implements Expr, Printable {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_PAREN;
  }

  @Override
  public Optional<Type> getType() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return new StringBuilder().append("(").append(expr.print(indent)).append(")").toString();
  }
}
