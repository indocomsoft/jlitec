package jlitec.ir3.expr.rval;

public record IntRvalExpr(int value) implements LiteralRvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.INT;
  }

  @Override
  public String print(int indent) {
    return Integer.toString(value);
  }
}
