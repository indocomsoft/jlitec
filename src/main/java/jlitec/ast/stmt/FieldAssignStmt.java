package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public class FieldAssignStmt implements Stmt {
  public final Expr lhsTarget;
  public final String lhsId;
  public final Expr rhs;

  /**
   * The only constructor.
   *
   * @param lhsTarget the expression that is the target in the left hand side.
   * @param lhsId the field identifier on the left hand side.
   * @param rhs the expression on the right hand side.
   */
  public FieldAssignStmt(Expr lhsTarget, String lhsId, Expr rhs) {
    this.lhsTarget = lhsTarget;
    this.lhsId = lhsId;
    this.rhs = rhs;
  }

  @Override
  public StmtType getType() {
    return StmtType.STMT_FIELD_ASSIGN;
  }
}
