package jlitec.ast.expr;

public class NullExpr implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NULL;
  }
}
