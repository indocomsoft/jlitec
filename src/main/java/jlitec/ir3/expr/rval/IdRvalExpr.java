package jlitec.ir3.expr.rval;

import java.util.Objects;

public record IdRvalExpr(String id) implements RvalExpr {
  public IdRvalExpr {
    Objects.requireNonNull(id);
  }

  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.ID;
  }

  @Override
  public String print(int indent) {
    return id;
  }
}
