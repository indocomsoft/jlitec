package jlitec.ast.expr;

public class IdExpr implements Expr {
  public final String id;

  public IdExpr(String id) {
    this.id = id;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_ID;
  }
}
