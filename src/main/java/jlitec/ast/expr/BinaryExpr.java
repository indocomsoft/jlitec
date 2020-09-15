package jlitec.ast.expr;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }
}
