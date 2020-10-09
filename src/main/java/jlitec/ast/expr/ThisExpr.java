package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record ThisExpr(TypeAnnotation typeAnnotation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_THIS;
  }
}
