package jlitec.ast.expr;

public class NewExpr implements Expr {
  public final String cname;

  public NewExpr(String cname) {
    this.cname = cname;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NEW;
  }
}
