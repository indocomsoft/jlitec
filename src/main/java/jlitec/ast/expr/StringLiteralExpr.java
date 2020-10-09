package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record StringLiteralExpr(String value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.STRING);
  }
}
