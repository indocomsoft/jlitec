package jlitec.ast.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory;
import jlitec.ast.Locatable;
import jlitec.ast.Printable;

/** An interface for all the different kinds of expressions in JLite. */
public interface Expr extends Printable, Locatable {
  /**
   * This is non-nullable.
   *
   * @return ExprType of the current class.
   */
  ExprType getExprType();

  Optional<TypeHint> getTypeHint();

  enum TypeHint {
    INT,
    STRING,
    BOOL;
  }
}
