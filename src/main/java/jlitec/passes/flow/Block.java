package jlitec.passes.flow;

import java.util.Collections;
import java.util.List;
import jlitec.ir3.stmt.Stmt;

public interface Block {
  record Basic(List<Stmt> stmtList) implements Block {
    public Basic {
      this.stmtList = Collections.unmodifiableList(stmtList);
    }
  }

  record Exit() implements Block {}
}
