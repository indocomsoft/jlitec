package jlitec.ir3.expr.rval;

public record StringRvalExpr() implements RvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.STRING;
  }
}
