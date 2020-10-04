package jlitec.ast.expr;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) implements Expr {
  public BinaryExpr(jlitec.parsetree.expr.BinaryExpr be) {
    this(
        BinaryOp.fromParseTree(be.op()),
        Expr.fromParseTree(be.lhs()),
        Expr.fromParseTree(be.rhs()));
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }
}
