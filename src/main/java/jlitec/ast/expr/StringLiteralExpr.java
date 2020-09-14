package jlitec.ast.expr;

public class StringLiteralExpr implements Expr {
  public final String value;

  public StringLiteralExpr(String value) {
    this.value = value;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }
}
