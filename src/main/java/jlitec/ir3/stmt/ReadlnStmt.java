package jlitec.ir3.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record ReadlnStmt(IdRvalExpr dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.READLN;
  }
}
