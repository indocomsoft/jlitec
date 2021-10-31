package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.Set;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.flow.Block;

public record BlockWithLive(Block block, Set<Node> liveIn, Set<Node> liveOut) {
  public BlockWithLive {
    liveIn = Collections.unmodifiableSet(liveIn);
    liveOut = Collections.unmodifiableSet(liveOut);
  }
}
