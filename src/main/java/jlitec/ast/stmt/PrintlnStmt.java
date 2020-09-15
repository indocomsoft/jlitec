package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record PrintlnStmt(Expr expr) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_PRINTLN;
  }
}
