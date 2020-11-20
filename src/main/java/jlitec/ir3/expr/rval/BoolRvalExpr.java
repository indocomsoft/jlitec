package jlitec.ir3.expr.rval;

public record BoolRvalExpr(boolean value) implements LiteralRvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.BOOL;
  }

  @Override
  public String print(int indent) {
    return value ? "true" : "false";
  }
}
