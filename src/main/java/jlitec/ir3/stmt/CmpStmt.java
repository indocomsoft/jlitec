package jlitec.ir3.stmt;

import jlitec.ast.expr.BinaryExpr;

public record CmpStmt(BinaryExpr condition, LabelStmt dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.CMP;
  }
}
