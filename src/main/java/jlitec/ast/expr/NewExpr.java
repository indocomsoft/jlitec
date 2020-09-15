package jlitec.ast.expr;

import java.util.Optional;

public record NewExpr(String cname) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NEW;
  }

  @Override
  public Optional<Type> getType() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return new StringBuilder().append("new ").append(cname).append("()").toString();
  }
}
