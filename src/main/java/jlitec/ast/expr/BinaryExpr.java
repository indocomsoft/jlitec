package jlitec.ast.expr;

import jlitec.ast.Printable;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) implements Expr, Printable {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }

  @Override
  public String print(int indent) {
    return new StringBuilder()
        .append(lhs.print(indent))
        .append(' ')
        .append(op.toString())
        .append(' ')
        .append(rhs.print(indent))
        .toString();
  }
}
