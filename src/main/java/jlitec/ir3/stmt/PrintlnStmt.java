package jlitec.ir3.stmt;

import jlitec.ir3.expr.rval.RvalExpr;

public record PrintlnStmt(RvalExpr rval) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.PRINTLN;
  }
}
