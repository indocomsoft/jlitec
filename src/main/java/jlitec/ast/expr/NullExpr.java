package jlitec.ast.expr;

import java.util.Optional;

public record NullExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NULL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return "null";
  }
}
