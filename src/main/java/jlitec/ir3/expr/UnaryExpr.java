package jlitec.ir3.expr;

import jlitec.ir3.expr.rval.RvalExpr;

public record UnaryExpr(UnaryOp op, RvalExpr rval) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.UNARY;
  }
}
