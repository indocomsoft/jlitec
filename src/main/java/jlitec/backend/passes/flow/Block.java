package jlitec.backend.passes.flow;

import java.util.Collections;
import java.util.List;
import jlitec.backend.passes.lower.stmt.LowerStmt;

public interface Block {
  enum Type {
    BASIC,
    EXIT;
  }

  Type type();

  record Basic(List<LowerStmt> lowerStmtList) implements Block {
    public Basic {
      lowerStmtList = Collections.unmodifiableList(lowerStmtList);
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
