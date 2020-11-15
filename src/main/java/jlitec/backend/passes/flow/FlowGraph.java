package jlitec.backend.passes.flow;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record FlowGraph(List<Block> blocks, Multimap<Integer, Integer> edges) {
  public FlowGraph {
    this.blocks = Collections.unmodifiableList(blocks);
    this.edges = Multimaps.unmodifiableMultimap(edges);
  }

  public String generateDot() {
    final var sb = new StringBuilder();
    sb.append("digraph G {\n");
    sb.append("  ENTRY -> B0;\n");
    for (int i = 0; i < blocks.size() - 1; i++) {
      final var bb = (Block.Basic) blocks.get(i);
      final var blockName = "B" + i;
      final var printed = bb.stmtList().stream().map(s -> s.print(0)).collect(Collectors.joining("\n"));
      sb.append("  ").append(blockName).append("[xlabel=\"").append(blockName).append("\", label=\"").append(StringEscapeUtils.escapeJava(printed)).append("\", shape=box];\n");
    }
    for (final var edgeEntry : edges.entries()) {
      final var src = edgeEntry.getKey();
      final var dst = edgeEntry.getValue();
      final var srcLabel = blocks.get(src).type() == Block.Type.EXIT ? "EXIT" : "B" + src;
      final var dstLabel = blocks.get(dst).type() == Block.Type.EXIT ? "EXIT" : "B" + dst;
      sb.append("  ").append(srcLabel).append(" -> ").append(dstLabel).append(";\n");
    }
    sb.append("  {rank = sink; EXIT;}\n");
    sb.append("}\n");
    return sb.toString();
  }
}
