package jlitec.ast.stmt;

import java.util.List;
import jlitec.ast.expr.Expr;

public record IfStmt(Expr condition, List<Stmt> thenStmtList, List<Stmt> elseStmtList)
    implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_IF;
  }
}
