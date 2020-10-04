package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record VarAssignStmt(String lhsId, Expr rhs) implements Stmt {
  public VarAssignStmt(jlitec.parsetree.stmt.VarAssignStmt vas) {
    this(vas.lhsId(), Expr.fromParseTree(vas.rhs()));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_VAR_ASSIGN;
  }
}
