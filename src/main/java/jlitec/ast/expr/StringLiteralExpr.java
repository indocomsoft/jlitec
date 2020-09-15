package jlitec.ast.expr;

public record StringLiteralExpr(String value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }
}
