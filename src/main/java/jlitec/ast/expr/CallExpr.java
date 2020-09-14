package jlitec.ast.expr;

import java.util.List;

public class CallExpr implements Expr {
  public final Expr target;
  public final List<Expr> args;

  public CallExpr(Expr target, List<Expr> args) {
    this.target = target;
    this.args = args;
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }
}
