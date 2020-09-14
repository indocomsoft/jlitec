package jlitec.ast.expr;

public class IntLiteralExpr implements Expr {
  public final int value;

  public IntLiteralExpr(int value) {
    this.value = value;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_INT_LITERAL;
  }
}
