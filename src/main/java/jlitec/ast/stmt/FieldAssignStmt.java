package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record FieldAssignStmt(Expr lhsTarget, String lhsId, Expr rhs) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_FIELD_ASSIGN;
  }
}
