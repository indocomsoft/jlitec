package jlitec.ast.expr;

import jlitec.ast.TypeAnnotation;

public record NewExpr(String cname) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_NEW;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Klass(cname);
  }
}
