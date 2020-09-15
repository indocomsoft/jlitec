package jlitec.ast.expr;

import jlitec.ast.Printable;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr extends Printable {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();
}
