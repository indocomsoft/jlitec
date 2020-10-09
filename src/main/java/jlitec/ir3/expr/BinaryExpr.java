package jlitec.ir3.expr;

import jlitec.ir3.expr.rval.RvalExpr;

public record BinaryExpr(BinaryOp op, RvalExpr lhs, RvalExpr rhs) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.BINARY;
  }

  @Override
  public String print(int indent) {
    return lhs.print(indent) + " " + op.print(indent) + " " + rhs.print(indent);
  }
}
