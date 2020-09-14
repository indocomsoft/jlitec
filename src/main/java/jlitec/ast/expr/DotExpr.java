package jlitec.ast.expr;

public class DotExpr implements Expr {
  public final Expr target;
  public final String id;

  public DotExpr(Expr target, String id) {
    this.target = target;
    this.id = id;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }
}
