package jlitec.ast.expr;

public record ThisExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_THIS;
  }

  @Override
  public String print(int indent) {
    return "this";
  }
}
