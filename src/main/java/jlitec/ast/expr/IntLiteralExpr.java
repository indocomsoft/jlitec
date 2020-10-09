package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record IntLiteralExpr(int value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_INT_LITERAL;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.INT);
  }
}
