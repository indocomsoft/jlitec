package jlitec.ast.expr;

import jlitec.ast.TypeAnnotable;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr extends TypeAnnotable {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();
}
