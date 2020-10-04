package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record PrintlnStmt(Expr expr) implements Stmt {
  public PrintlnStmt(jlitec.parsetree.stmt.PrintlnStmt ps) {
    this(Expr.fromParseTree(ps.expr()));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_PRINTLN;
  }
}
