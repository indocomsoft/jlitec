package jlitec.ast.stmt;

import java.util.List;
import jlitec.ast.expr.Expr;

public class CallStmt implements Stmt {
  public final Expr target;
  public final List<Expr> args;

  public CallStmt(Expr target, List<Expr> args) {
    this.target = target;
    this.args = args;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_CALL;
  }
}
