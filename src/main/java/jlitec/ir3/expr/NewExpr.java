package jlitec.ir3.expr;

public record NewExpr(String cname) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.NEW;
  }

  @Override
  public String print(int indent) {
    return "new " + cname + "()";
  }
}
