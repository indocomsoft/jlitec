package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs, TypeAnnotation typeAnnotation)
    implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }
}
