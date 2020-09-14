package jlitec.ast.expr;

public class UnaryExpr implements Expr {
  public final UnaryOp op;
  public final Expr expr;

  public UnaryExpr(UnaryOp op, Expr expr) {
    this.op = op;
    this.expr = expr;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_UNARY;
  }
}
