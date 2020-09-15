package jlitec.ast.expr;

public record UnaryExpr(UnaryOp op, Expr expr) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_UNARY;
  }
}
