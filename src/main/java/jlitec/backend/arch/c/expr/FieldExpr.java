package jlitec.backend.arch.c.expr;

public record FieldExpr(String target, String field) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.FIELD;
  }

  @Override
  public String print(int indent) {
    return target + "->" + field;
  }
}
