package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.Set;
import jlitec.ir3.stmt.Stmt;

public record StmtWithLiveInfo(Stmt stmt, Set<String> liveIn, Set<String> liveOut) {
  public StmtWithLiveInfo {
    this.liveIn = Collections.unmodifiableSet(liveIn);
    this.liveOut = Collections.unmodifiableSet(liveOut);
  }
}
