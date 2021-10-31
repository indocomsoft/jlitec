package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.Set;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.lower.stmt.LowerStmt;

public record LowerStmtWithLive(LowerStmt lowerStmt, Set<Node> liveIn, Set<Node> liveOut) {
  public LowerStmtWithLive {
    liveIn = Collections.unmodifiableSet(liveIn);
    liveOut = Collections.unmodifiableSet(liveOut);
  }
}
