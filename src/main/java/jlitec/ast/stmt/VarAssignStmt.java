package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record VarAssignStmt(String lhsId, Expr rhs) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_VAR_ASSIGN;
  }
}
