package jlitec.parsetree.stmt;

import jlitec.Printable;
import jlitec.parsetree.Locatable;

/** An interface for all the different kinds of Statements in JLite. */
public interface Stmt extends Printable, Locatable {
  /**
   * This is non-nullable.
   *
   * @return StmtType of the current Statement.
   */
  StmtType getStmtType();
}
