package jlitec.ast.stmt;

/** An interface for all the different kinds of Statements in JLite. */
public interface Stmt {
  /**
   * This is non-nullable.
   *
   * @return StmtType of the current Statement.
   */
  StmtType getStmtType();

  static Stmt fromParseTree(jlitec.parsetree.stmt.Stmt s) {
    return switch (s.getStmtType()) {
      case STMT_IF -> new IfStmt((jlitec.parsetree.stmt.IfStmt) s);
      case STMT_WHILE -> new WhileStmt((jlitec.parsetree.stmt.WhileStmt) s);
      case STMT_READLN -> new ReadlnStmt(((jlitec.parsetree.stmt.ReadlnStmt) s).id());
      case STMT_PRINTLN -> new PrintlnStmt((jlitec.parsetree.stmt.PrintlnStmt) s);
      case STMT_VAR_ASSIGN -> new VarAssignStmt((jlitec.parsetree.stmt.VarAssignStmt) s);
      case STMT_FIELD_ASSIGN -> new FieldAssignStmt((jlitec.parsetree.stmt.FieldAssignStmt) s);
      case STMT_CALL -> new CallStmt((jlitec.parsetree.stmt.CallStmt) s);
      case STMT_RETURN -> new ReturnStmt((jlitec.parsetree.stmt.ReturnStmt) s);
    };
  }
}
