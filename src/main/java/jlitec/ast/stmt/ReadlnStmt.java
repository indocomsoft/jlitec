package jlitec.ast.stmt;

public record ReadlnStmt(String id) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_READLN;
  }
}
