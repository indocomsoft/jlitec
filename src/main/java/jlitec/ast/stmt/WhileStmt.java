package jlitec.ast.stmt;

import java.util.List;
import jlitec.ast.expr.Expr;

public class WhileStmt implements Stmt {
  public final Expr condition;
  public final List<Stmt> stmtList;

  public WhileStmt(Expr condition, List<Stmt> stmtList) {
    this.condition = condition;
    this.stmtList = stmtList;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_WHILE;
  }
}
