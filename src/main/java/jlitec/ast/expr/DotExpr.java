package jlitec.ast.expr;

import java.util.Optional;

public record DotExpr(Expr target, String id) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }

  @Override
  public Optional<Type> getType() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    return target.print(indent) + '.' + id;
  }
}
