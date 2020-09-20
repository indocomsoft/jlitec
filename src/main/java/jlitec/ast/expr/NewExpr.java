package jlitec.ast.expr;

import java.util.Optional;

public record NewExpr(String cname) implements Expr {
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
