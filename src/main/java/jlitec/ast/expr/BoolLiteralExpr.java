package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record BoolLiteralExpr(boolean value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BOOL_LITERAL;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.BOOL);
  }
}
