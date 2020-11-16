package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.Set;
import jlitec.backend.passes.flow.Block;

public record BlockWithLive(Block block, Set<Node> liveIn, Set<Node> liveOut) {
  public BlockWithLive {
    this.liveIn = Collections.unmodifiableSet(liveIn);
    this.liveOut = Collections.unmodifiableSet(liveOut);
  }
}
