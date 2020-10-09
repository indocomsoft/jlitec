package jlitec.ir3.expr.rval;

public record BoolRvalExpr(boolean value) implements RvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.BOOL;
  }
}