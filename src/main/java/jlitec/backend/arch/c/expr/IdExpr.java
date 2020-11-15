package jlitec.backend.arch.c.expr;

public record IdExpr(String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.ID;
  }

  @Override
  public String print(int indent) {
    return id;
  }
}
