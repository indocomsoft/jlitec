package jlitec.ast.expr;

public record BoolLiteralExpr(boolean value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BOOL_LITERAL;
  }
}
