package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

import java.util.List;
import java.util.stream.Collectors;

public record CallExpr(Expr target, List<Expr> args, TypeAnnotation typeAnnotation) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }
}
