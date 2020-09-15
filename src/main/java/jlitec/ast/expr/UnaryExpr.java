package jlitec.ast.expr;

public record UnaryExpr(UnaryOp op, Expr expr) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_UNARY;
  }

  @Override
  public String print(int indent) {
    return new StringBuilder().append(op.toString()).append(expr.print(indent)).toString();
  }
}
