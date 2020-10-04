package jlitec.ast.expr;

import java.util.List;
import java.util.stream.Collectors;

public record CallExpr(Expr target, List<Expr> args) implements Expr {
  public CallExpr(jlitec.parsetree.expr.CallExpr ce) {
    this(
        Expr.fromParseTree(ce.target()),
        ce.args().stream().map(Expr::fromParseTree).collect(Collectors.toUnmodifiableList()));
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }
}
