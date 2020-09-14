package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public class ReturnStmt implements Stmt {
  public final Expr expr;

  public ReturnStmt(Expr expr) {
    this.expr = expr;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_RETURN;
  }
}
