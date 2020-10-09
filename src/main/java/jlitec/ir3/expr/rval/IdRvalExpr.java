package jlitec.ir3.expr.rval;

public record IdRvalExpr(String id) implements RvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.ID;
  }

  @Override
  public String print(int indent) {
    return id;
  }
}
