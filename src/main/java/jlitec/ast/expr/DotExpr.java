package jlitec.ast.expr;

public record DotExpr(Expr target, String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }

  @Override
  public String print(int indent) {
    return new StringBuilder().append(target.print(indent)).append('.').append(id).toString();
  }
}
