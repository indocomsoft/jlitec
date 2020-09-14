package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public class VarAssignStmt implements Stmt {
  public final String lhsId;
  public final Expr rhs;

  public VarAssignStmt(String lhsId, Expr rhs) {
    this.lhsId = lhsId;
    this.rhs = rhs;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_VAR_ASSIGN;
  }
}
