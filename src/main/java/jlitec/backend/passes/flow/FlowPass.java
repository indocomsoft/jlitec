package jlitec.backend.passes.flow;

import com.google.common.collect.HashMultimap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.LabelLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;

public class FlowPass implements Pass<List<LowerStmt>, FlowGraph> {
  @Override
  public FlowGraph pass(List<LowerStmt> lowerStmtList) {
    final var usedLabels =
        lowerStmtList.stream()
            .flatMap(s -> s instanceof LabelLowerStmt ls ? Stream.of(ls.label()) : Stream.empty())
            .collect(Collectors.toUnmodifiableSet());
    final var leaders = new BitSet(lowerStmtList.size());

    // The first three-address instruction is a leader.
    leaders.set(0);

    for (int i = 0; i < lowerStmtList.size(); i++) {
      final var stmt = lowerStmtList.get(i);
      switch (stmt.stmtExtensionType()) {
          // Any instruction that is the target of a conditional or unconditional jump is a leader.
        case LABEL -> {
          final var ls = (LabelLowerStmt) stmt;
          if (usedLabels.contains(ls.label())) {
            leaders.set(i);
          }
        }
          // Any instruction that immediately follows a conditional or unconditional jump is a
          // leader.
        case CMP, GOTO, RETURN -> {
          if (i + 1 < lowerStmtList.size()) {
            leaders.set(i + 1);
          }
        }
      }
    }

    final var blocks = new ArrayList<Block>();
    for (int i = leaders.nextSetBit(0); i != -1; i = leaders.nextSetBit(i + 1)) {
      final var next = leaders.nextSetBit(i + 1);
      final var startOfNextBlock = next == -1 ? lowerStmtList.size() : next;
      blocks.add(new Block.Basic(lowerStmtList.subList(i, startOfNextBlock)));
    }

    final var labelToBlock = new HashMap<String, Integer>();
    blocks.add(new Block.Exit());
    for (int i = 0; i < blocks.size() - 1; i++) {
      final var b = (Block.Basic) blocks.get(i);
      final var firstStmt = b.lowerStmtList().get(0);
      if (firstStmt instanceof LabelLowerStmt ls) {
        labelToBlock.put(ls.label(), i);
      }
    }

    final var edges = HashMultimap.<Integer, Integer>create();
    for (int i = 0; i < blocks.size() - 1; i++) {
      final var block = (Block.Basic) blocks.get(i);
      final var lastStmt = block.lowerStmtList().get(block.lowerStmtList().size() - 1);
      switch (lastStmt.stmtExtensionType()) {
        case GOTO -> {
          final var gs = (GotoLowerStmt) lastStmt;
          final var destBlock = labelToBlock.get(gs.dest());
          edges.put(i, destBlock);
        }
        case CMP -> {
          final var cs = (CmpLowerStmt) lastStmt;
          final var destBlock = labelToBlock.get(cs.dest());
          edges.put(i, destBlock);
          if (i + 1 < blocks.size()) {
            edges.put(i, i + 1);
          }
        }
        case RETURN -> {
          edges.put(i, blocks.size() - 1);
        }
        default -> {
          if (i + 1 < blocks.size()) {
            edges.put(i, i + 1);
          }
        }
      }
    }

    return new FlowGraph(blocks, edges);
  }
}
