package jlitec.ir3.expr.rval;

public record IntRvalExpr(int value) implements RvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.INT;
  }
}
