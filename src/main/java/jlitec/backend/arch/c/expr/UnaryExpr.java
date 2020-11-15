package jlitec.backend.arch.c.expr;

public record UnaryExpr(UnaryOp op, Expr expr) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.UNARY;
  }

  @Override
  public String print(int indent) {
    return op.print(indent) + expr.print(indent);
  }
}
