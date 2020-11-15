package jlitec.backend.arch.c.expr;

public record AddrExpr(String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.ADDR;
  }

  @Override
  public String print(int indent) {
    return "&" + id;
  }
}
