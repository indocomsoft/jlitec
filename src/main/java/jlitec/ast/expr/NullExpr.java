package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record NullExpr() implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NULL;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Null();
  }
}
