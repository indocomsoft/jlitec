package jlitec.ast.stmt;

import java.util.List;
import jlitec.ast.expr.Expr;

public record WhileStmt(Expr condition, List<Stmt> stmtList) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_WHILE;
  }
}
