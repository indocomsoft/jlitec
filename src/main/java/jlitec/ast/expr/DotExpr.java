package jlitec.ast.expr;

public record DotExpr(Expr target, String id) implements Expr {
  public DotExpr(jlitec.parsetree.expr.DotExpr de) {
    this(Expr.fromParseTree(de.target()), de.id());
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }
}
