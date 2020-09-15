package jlitec.ast.expr;

public record NullExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NULL;
  }

  @Override
  public String print(int indent) {
    return "null";
  }
}
