package jlitec.ast.expr;

import java.util.Optional;

public record BoolLiteralExpr(boolean value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BOOL_LITERAL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.of(TypeHint.BOOL);
  }

  @Override
  public String print(int indent) {
    return value ? "true" : "false";
  }
}
