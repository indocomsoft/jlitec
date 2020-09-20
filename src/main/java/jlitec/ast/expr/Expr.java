package jlitec.ast.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory;
import jlitec.ast.Printable;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr extends Printable {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();

  Optional<TypeHint> getTypeHint();

  ComplexSymbolFactory.Location leftLocation();

  ComplexSymbolFactory.Location rightLocation();

  enum TypeHint {
    INT,
    STRING,
    BOOL;
  }
}
