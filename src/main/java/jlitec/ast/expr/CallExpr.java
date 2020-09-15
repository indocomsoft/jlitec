package jlitec.ast.expr;

import java.util.List;

public record CallExpr(Expr target, List<Expr> args) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }
}
