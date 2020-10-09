package jlitec.ir3.expr;

import jlitec.ir3.expr.rval.RvalExpr;

public record UnaryExpr(UnaryOp op, RvalExpr rval) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.UNARY;
  }

  @Override
  public String print(int indent) {
    return op.print(indent) + rval.print(indent);
  }
}
