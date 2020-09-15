package jlitec.ast.stmt;

import java.util.List;
import jlitec.ast.expr.Expr;

public record CallStmt(Expr target, List<Expr> args) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_CALL;
  }
}
