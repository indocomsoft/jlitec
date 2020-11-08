package jlitec.passes.flow;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collections;
import java.util.List;

public record FlowGraph(List<Block> blocks, Multimap<Integer, Integer> edges) {
  public FlowGraph {
    this.blocks = Collections.unmodifiableList(blocks);
    this.edges = Multimaps.unmodifiableMultimap(edges);
  }
}
