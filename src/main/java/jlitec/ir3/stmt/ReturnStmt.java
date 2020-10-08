package jlitec.ir3.stmt;

import java.util.Optional;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record ReturnStmt(Optional<IdRvalExpr> maybeValue) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.RETURN;
  }
}
