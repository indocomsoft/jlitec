package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.Set;
import jlitec.backend.passes.lower.stmt.LowerStmt;

public record LowerStmtWithLive(LowerStmt lowerStmt, Set<Node> liveIn, Set<Node> liveOut) {
  public LowerStmtWithLive {
    this.liveIn = Collections.unmodifiableSet(liveIn);
    this.liveOut = Collections.unmodifiableSet(liveOut);
  }
}
