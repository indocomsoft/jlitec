package jlitec.ast.expr;

public record DotExpr(Expr target, String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }
}
