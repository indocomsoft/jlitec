package jlitec.ir3.codegen;

import java.util.Collections;
import java.util.List;
import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.stmt.Stmt;

public record RvalChunk(RvalExpr rval, List<Stmt> stmtList) {
  public RvalChunk {
    this.stmtList = Collections.unmodifiableList(stmtList);
  }
}
