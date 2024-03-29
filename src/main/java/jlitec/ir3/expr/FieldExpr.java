package jlitec.ir3.expr;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record FieldExpr(IdRvalExpr target, String field) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.FIELD;
  }

  @Override
  public String print(int indent) {
    return target.print(indent) + "." + field;
  }
}
