package jlitec.ast.stmt;

import jlitec.ast.Locatable;
import jlitec.ast.Printable;

/** An interface for all the different kinds of Statements in JLite. */
public interface Stmt extends Printable, Locatable {
  /**
   * This is non-nullable.
   *
   * @return StmtType of the current Statement.
   */
  StmtType getStmtType();
}
