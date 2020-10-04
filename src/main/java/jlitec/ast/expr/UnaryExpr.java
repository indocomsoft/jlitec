package jlitec.ast.expr;

public record UnaryExpr(UnaryOp op, Expr expr) implements Expr {
  public UnaryExpr(jlitec.parsetree.expr.UnaryExpr e) {
    this(UnaryOp.fromParseTree(e.op()), Expr.fromParseTree(e.expr()));
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_UNARY;
  }
}
