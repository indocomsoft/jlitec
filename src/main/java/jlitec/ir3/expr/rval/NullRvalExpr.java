package jlitec.ir3.expr.rval;

public record NullRvalExpr() implements RvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.NULL;
  }
}
