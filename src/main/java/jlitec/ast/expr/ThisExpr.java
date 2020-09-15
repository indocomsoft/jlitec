package jlitec.ast.expr;

import java.util.Optional;

public record ThisExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_THIS;
  }

  @Override
  public Optional<Type> getType() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return "this";
  }
}
