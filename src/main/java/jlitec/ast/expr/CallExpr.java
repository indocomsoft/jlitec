package jlitec.ast.expr;

import java.util.List;
import jlitec.ast.MethodReference;
import jlitec.ast.TypeAnnotation;

public record CallExpr(
    Expr target, List<Expr> args, TypeAnnotation typeAnnotation, MethodReference methodReference)
    implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }
}
