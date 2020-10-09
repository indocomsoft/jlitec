package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record IdExpr(String id, TypeAnnotation typeAnnotation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_ID;
  }
}
