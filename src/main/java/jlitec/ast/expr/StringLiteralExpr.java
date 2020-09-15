package jlitec.ast.expr;

public record StringLiteralExpr(String value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }

  @Override
  public String print(int indent) {
    return new StringBuilder().append('"').append(value).append('"').toString();
  }
}
