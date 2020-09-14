package jlitec.ast.expr;

public class ThisExpr implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_THIS;
  }
}
