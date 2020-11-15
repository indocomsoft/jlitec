package jlitec.backend.arch.c.expr;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.BINARY;
  }

  @Override
  public String print(int indent) {
    return lhs.print(indent) + " " + op.print(indent) + " " + rhs.print(indent);
  }
}
