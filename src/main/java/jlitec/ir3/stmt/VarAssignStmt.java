package jlitec.ir3.stmt;

import jlitec.ir3.expr.Expr;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record VarAssignStmt(IdRvalExpr lhs, Expr rhs) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.VAR_ASSIGN;
  }
}
