package jlitec.backend.passes.flow;

import java.util.Collections;
import java.util.List;
import jlitec.ir3.stmt.Stmt;

public interface Block {
  enum Type {
    BASIC,
    EXIT;
  }

  Type type();

  record Basic(List<Stmt> stmtList) implements Block {
    public Basic {
      this.stmtList = Collections.unmodifiableList(stmtList);
    }

    @Override
    public Type type() {
      return Type.BASIC;
    }
  }

  record Exit() implements Block {
    @Override
    public Type type() {
      return Type.EXIT;
    }
  }
}
