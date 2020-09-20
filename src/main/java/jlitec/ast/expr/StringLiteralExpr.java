package jlitec.ast.expr;

import java.util.Optional;

public record StringLiteralExpr(String value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.of(TypeHint.STRING);
  }

  @Override
  public String print(int indent) {
    return '"' + value + '"';
  }
}
