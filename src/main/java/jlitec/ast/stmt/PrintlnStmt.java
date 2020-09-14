package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public class PrintlnStmt implements Stmt {
  public final Expr expr;

  public PrintlnStmt(Expr expr) {
    this.expr = expr;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_PRINTLN;
  }
}
