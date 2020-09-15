package jlitec.ast.expr;

import jlitec.ast.Printable;

import java.util.Optional;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr extends Printable {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();

  Optional<Type> getType();

  public enum Type {
    INT,
    STRING,
    BOOL;
  }
}
