package jlitec.ast.expr;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();
}
