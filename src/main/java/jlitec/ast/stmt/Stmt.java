package jlitec.ast.stmt;

/** An interface for all the different kinds of Statements in JLite. */
public interface Stmt {
  /**
   * This is non-nullable.
   *
   * @return StmtType of the current Statement.
   */
  StmtType getStmtType();
}
