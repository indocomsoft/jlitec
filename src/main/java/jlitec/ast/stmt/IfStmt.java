package jlitec.ast.stmt;

import java.util.List;
import jlitec.ast.expr.Expr;

public class IfStmt implements Stmt {
  public final Expr condition;
  public final List<Stmt> thenStmtList;
  public final List<Stmt> elseStmtList;

  /**
   * The only constructor.
   *
   * @param condition Condition for the if statement.
   * @param thenStmtList list of statements to perform if condition is true.
   * @param elseStmtList list of statements ot perform if condition is false.
   */
  public IfStmt(Expr condition, List<Stmt> thenStmtList, List<Stmt> elseStmtList) {
    this.condition = condition;
    this.thenStmtList = thenStmtList;
    this.elseStmtList = elseStmtList;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_IF;
  }
}
