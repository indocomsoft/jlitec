package jlitec.ir3.expr;

public record NewExpr(String cname) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.NEW;
  }
}
