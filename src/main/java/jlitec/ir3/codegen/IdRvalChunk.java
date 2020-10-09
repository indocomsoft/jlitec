package jlitec.ir3.codegen;

import java.util.Collections;
import java.util.List;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.stmt.Stmt;

public record IdRvalChunk(IdRvalExpr idRval, List<Stmt> stmtList) {
  public IdRvalChunk {
    this.stmtList = Collections.unmodifiableList(stmtList);
  }
}
