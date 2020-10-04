package jlitec.ast.expr;

public record ThisExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_THIS;
  }
}
