package jlitec.ast.expr;

import java.util.Optional;

public record BoolLiteralExpr(boolean value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BOOL_LITERAL;
  }

  @Override
  public Optional<Type> getType() {
    return Optional.of(Type.BOOL);
  }

  @Override
  public String print(int indent) {
    return value ? "true" : "false";
  }
}
