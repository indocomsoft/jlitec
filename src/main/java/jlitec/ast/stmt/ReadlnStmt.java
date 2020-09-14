package jlitec.ast.stmt;

public class ReadlnStmt implements Stmt {
  public final String id;

  public ReadlnStmt(String id) {
    this.id = id;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_READLN;
  }
}
