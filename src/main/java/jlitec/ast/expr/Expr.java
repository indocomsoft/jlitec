package jlitec.ast.expr;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();

  static Expr fromParseTree(jlitec.parsetree.expr.Expr e) {
    return switch (e.getExprType()) {
      case EXPR_INT_LITERAL -> new IntLiteralExpr(
          ((jlitec.parsetree.expr.IntLiteralExpr) e).value());
      case EXPR_STRING_LITERAL -> new StringLiteralExpr(
          ((jlitec.parsetree.expr.StringLiteralExpr) e).value());
      case EXPR_BOOL_LITERAL -> new BoolLiteralExpr(
          ((jlitec.parsetree.expr.BoolLiteralExpr) e).value());
      case EXPR_BINARY -> new BinaryExpr((jlitec.parsetree.expr.BinaryExpr) e);
      case EXPR_UNARY -> new UnaryExpr((jlitec.parsetree.expr.UnaryExpr) e);
      case EXPR_DOT -> new DotExpr((jlitec.parsetree.expr.DotExpr) e);
      case EXPR_CALL -> new CallExpr((jlitec.parsetree.expr.CallExpr) e);
      case EXPR_THIS -> new ThisExpr();
      case EXPR_ID -> new IdExpr(((jlitec.parsetree.expr.IdExpr) e).id());
      case EXPR_NEW -> new NewExpr(((jlitec.parsetree.expr.NewExpr) e).cname());
      case EXPR_NULL -> new NullExpr();
      case EXPR_PAREN -> fromParseTree(((jlitec.parsetree.expr.ParenExpr) e).expr());
    };
  }
}
