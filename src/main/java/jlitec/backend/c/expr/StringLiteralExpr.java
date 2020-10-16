package jlitec.backend.c.expr;

public record StringLiteralExpr(String value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.STRING_LITERAL;
  }

  @Override
  public String print(int indent) {
    return "\"" + value + "\"";
  }
}
