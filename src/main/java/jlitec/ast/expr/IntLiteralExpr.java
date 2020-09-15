package jlitec.ast.expr;

public record IntLiteralExpr(int value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_INT_LITERAL;
  }

  @Override
  public String print(int indent) {
    return Integer.toString(value);
  }
}
