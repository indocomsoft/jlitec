package jlitec.ast.expr;

import java.util.Collections;
import java.util.List;

public record CallExpr(Expr target, List<Expr> args) implements Expr {
  public CallExpr {
    this.args = Collections.unmodifiableList(args);
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }
}
