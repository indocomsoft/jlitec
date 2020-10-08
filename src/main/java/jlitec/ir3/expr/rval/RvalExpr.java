package jlitec.ir3.expr.rval;

import jlitec.ir3.expr.Expr;
import jlitec.ir3.expr.ExprType;

public interface RvalExpr extends Expr {
  RvalExprType getRvalExprType();

  @Override
  default ExprType getExprType() {
    return ExprType.RVAL;
  }
}
