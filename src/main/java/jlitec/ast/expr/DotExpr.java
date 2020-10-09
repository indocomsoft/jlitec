package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record DotExpr(Expr target, String id, TypeAnnotation typeAnnotation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_DOT;
  }
}
