package jlitec.backend.c.expr;

public record BoolLiteralExpr(boolean value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.BOOL_LITERAL;
  }

  @Override
  public String print(int indent) {
    return value ? "true" : "false";
  }
}
