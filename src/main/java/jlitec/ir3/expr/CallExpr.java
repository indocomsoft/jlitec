package jlitec.ir3.expr;

import java.util.List;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public record CallExpr(IdRvalExpr target, List<RvalExpr> args) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.CALL;
  }
}
