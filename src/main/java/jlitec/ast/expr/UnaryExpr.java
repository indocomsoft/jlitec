package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record UnaryExpr(UnaryOp op, Expr expr, TypeAnnotation typeAnnotation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_UNARY;
  }
}
