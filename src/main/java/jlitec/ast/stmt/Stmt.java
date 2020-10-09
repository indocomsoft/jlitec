package jlitec.ast.stmt;

import jlitec.ast.TypeAnnotable;

/** An interface for all the different kinds of Statements in JLite. */
public interface Stmt extends TypeAnnotable {
  /**
   * This is non-nullable.
   *
   * @return StmtType of the current Statement.
   */
  StmtType getStmtType();
}
