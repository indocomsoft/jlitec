package jlitec.ast.expr;

public record BoolLiteralExpr(boolean value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BOOL_LITERAL;
  }

  @Override
  public String print(int indent) {
    return value ? "true" : "false";
  }
}
