package jlitec.ast.expr;

import java.util.Optional;

public record IdExpr(String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_ID;
  }

  @Override
  public Optional<Type> getType() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return id;
  }
}
