package jlitec.backend.c.expr;

public record IntLiteralExpr(int value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.INT_LITERAL;
  }

  @Override
  public String print(int indent) {
    return Integer.toString(value);
  }
}
