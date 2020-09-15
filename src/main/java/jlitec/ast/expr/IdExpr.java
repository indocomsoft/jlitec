package jlitec.ast.expr;

public record IdExpr(String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_ID;
  }
}
