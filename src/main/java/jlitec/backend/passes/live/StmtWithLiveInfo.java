package jlitec.backend.passes.live;

import jlitec.ir3.Var;
import jlitec.ir3.stmt.Stmt;

import java.util.Collections;
import java.util.Set;

public record StmtWithLiveInfo(Stmt stmt, Set<Var> liveIn, Set<Var> liveOut) {
  public StmtWithLiveInfo {
    this.liveIn = Collections.unmodifiableSet(liveIn);
    this.liveOut = Collections.unmodifiableSet(liveOut);
  }
}
