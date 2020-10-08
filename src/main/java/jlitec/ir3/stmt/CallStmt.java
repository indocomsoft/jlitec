package jlitec.ir3.stmt;

import java.util.List;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public record CallStmt(IdRvalExpr target, List<RvalExpr> args) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.CALL;
  }
}
