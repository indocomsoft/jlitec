package jlitec.ast.expr;

import java.util.Optional;

public record IntLiteralExpr(int value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_INT_LITERAL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.of(TypeHint.INT);
  }

  @Override
  public String print(int indent) {
    return Integer.toString(value);
  }
}
