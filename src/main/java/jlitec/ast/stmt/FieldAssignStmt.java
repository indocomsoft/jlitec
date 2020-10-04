package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record FieldAssignStmt(Expr lhsTarget, String lhsId, Expr rhs) implements Stmt {
  public FieldAssignStmt(jlitec.parsetree.stmt.FieldAssignStmt fas) {
    this(Expr.fromParseTree(fas.lhsTarget()), fas.lhsId(), Expr.fromParseTree(fas.rhs()));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_FIELD_ASSIGN;
  }
}
