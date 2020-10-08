package jlitec.parsetree.expr;

import java.util.Optional;
import jlitec.Printable;
import jlitec.parsetree.Locatable;

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
