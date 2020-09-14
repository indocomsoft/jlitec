package jlitec.ast.expr;

public class BoolLiteralExpr implements Expr {
  public final boolean value;

  public BoolLiteralExpr(boolean value) {
    this.value = value;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BOOL_LITERAL;
  }
}
