package jlitec.ast.expr;

public class BinaryExpr implements Expr {
  public final BinaryOp op;
  public final Expr lhs;
  public final Expr rhs;

  /**
   * The only constructor.
   *
   * @param op the binary operator.
   * @param lhs the left hand side operand.
   * @param rhs the right hand side operand.
   */
  public BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) {
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }
}
