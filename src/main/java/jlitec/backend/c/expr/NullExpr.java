package jlitec.backend.c.expr;

public record NullExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.NULL;
  }

  @Override
  public String print(int indent) {
    return "NULL";
  }
}
