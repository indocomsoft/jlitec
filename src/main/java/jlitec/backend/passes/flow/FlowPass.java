package jlitec.backend.passes.flow;

import com.google.common.collect.HashMultimap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.ir3.Program;
import jlitec.ir3.stmt.CmpStmt;
import jlitec.ir3.stmt.GotoStmt;
import jlitec.ir3.stmt.LabelStmt;
import jlitec.ir3.stmt.ReturnStmt;
import jlitec.ir3.stmt.Stmt;
import jlitec.backend.passes.Pass;

public class FlowPass implements Pass<jlitec.ir3.Program, ProgramWithFlow> {
  @Override
  public ProgramWithFlow pass(Program input) {
    final var result = new HashMap<jlitec.ir3.Method, FlowGraph>();
    for (final var ir3Method : input.methodList()) {
      result.put(ir3Method, process(ir3Method.stmtList()));
    }
    return new ProgramWithFlow(input, result);
  }

  private FlowGraph process(List<Stmt> stmtList) {
    final var usedLabels =
        stmtList.stream()
            .flatMap(s -> s instanceof LabelStmt ls ? Stream.of(ls.label()) : Stream.empty())
            .collect(Collectors.toUnmodifiableSet());
    final var leaders = new BitSet(stmtList.size());

    // The first three-address instruction is a leader.
    leaders.set(0);

    for (int i = 0; i < stmtList.size(); i++) {
      final var stmt = stmtList.get(i);
      switch (stmt.getStmtType()) {
          // Any instruction that is the target of a conditional or unconditional jump is a leader.
        case LABEL -> {
          final var ls = (LabelStmt) stmt;
          if (usedLabels.contains(ls.label())) {
            leaders.set(i);
          }
        }
          // Any instruction that immediately follows a conditional or unconditional jump is a
          // leader.
        case CMP, GOTO -> {
          if (i + 1 < stmtList.size()) {
            leaders.set(i + 1);
          }
        }
      }
    }

    final var blocks = new ArrayList<Block>();
    for (int i = leaders.nextSetBit(0); i != -1; i = leaders.nextSetBit(i + 1)) {
      final var next = leaders.nextSetBit(i + 1);
      final var startOfNextBlock = next == -1 ? stmtList.size() : next;
      blocks.add(new Block.Basic(stmtList.subList(i, startOfNextBlock)));
    }

    final var labelToBlock = new HashMap<String, Integer>();
    blocks.add(new Block.Exit());
    for (int i = 0; i < blocks.size() - 1; i++) {
      final var b = (Block.Basic) blocks.get(i);
      final var firstStmt = b.stmtList().get(0);
      if (firstStmt instanceof LabelStmt ls) {
        labelToBlock.put(ls.label(), i);
      }
    }

    final var edges = HashMultimap.<Integer, Integer>create();
    for (int i = 0; i < blocks.size() - 1; i++) {
      final var block = (Block.Basic) blocks.get(i);
      for (final var stmt : block.stmtList()) {
        if (stmt instanceof ReturnStmt) {
          edges.put(i, blocks.size() - 1);
          break;
        }
      }
      final var lastStmt = block.stmtList().get(block.stmtList().size() - 1);
      switch (lastStmt.getStmtType()) {
        case GOTO -> {
          final var gs = (GotoStmt) lastStmt;
          final var destBlock = labelToBlock.get(gs.dest().label());
          edges.put(i, destBlock);
        }
        case CMP -> {
          final var cs = (CmpStmt) lastStmt;
          final var destBlock = labelToBlock.get(cs.dest().label());
          edges.put(i, destBlock);
          if (i + 1 < blocks.size()) {
            edges.put(i, i + 1);
          }
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
