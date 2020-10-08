package jlitec.ir3.stmt;

public record GotoStmt(LabelStmt dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.GOTO;
  }
}
