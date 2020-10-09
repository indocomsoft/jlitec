package jlitec.ir3.expr.rval;

public record StringRvalExpr(String value) implements RvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.STRING;
  }

  @Override
  public String print(int indent) {
    return "\"" + value + "\"";
  }
}
