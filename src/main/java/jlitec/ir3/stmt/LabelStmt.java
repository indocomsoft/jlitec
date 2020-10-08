package jlitec.ir3.stmt;

public record LabelStmt(String label) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.LABEL;
  }
}
