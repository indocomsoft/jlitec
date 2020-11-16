package jlitec.backend.passes.regalloc.arm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.regalloc.MethodWithRegAlloc;
import jlitec.backend.passes.regalloc.RegAllocPass;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.stmt.Stmt;
import jlitec.ir3.stmt.VarAssignStmt;

public class ARMRegAlloc implements RegAllocPass<Register> {
  private static int NUM_REG = 13;

  private SetMultimap<String, Stmt> moveList;
  private List<Stmt> worklistMoves;
  private SetMultimap<String, String> adjSet;

  @Override
  public MethodWithRegAlloc<Register> pass(MethodWithFlow input) {
    moveList = HashMultimap.create();
    worklistMoves = new ArrayList<>();
    adjSet = HashMultimap.create();

    final var livePass = new LivePass();

    // LivenessAnalysis()
    final var livePassOutput = livePass.pass(input);

    // Build()
    for (final var blockWithLive : livePassOutput.blockWithLiveList()) {
      if (blockWithLive.block().type() == Block.Type.EXIT) continue;
      final var block = (Block.Basic) blockWithLive.block();
      var live = new HashSet<>(blockWithLive.liveOut());
      for (final var stmt : Lists.reverse(block.stmtList())) {
        final var defUse = LivePass.calculateDefUse(stmt);
        if (isMoveStmt(stmt)) {
          live.removeAll(defUse.use());
          for (final var n : Sets.union(defUse.def(), defUse.use())) {
            moveList.put(n, stmt);
          }
          worklistMoves.add(stmt);
        }
        live.addAll(defUse.def());
        for (final var d : defUse.def()) {
          for (final var l : live) {
            addEdge(l, d);
          }
        }
      }
    }

    return null;
  }

  private void addEdge(String u, String v) {
    if (adjSet.containsEntry(u, v) || u.equals(v)) {
      return;
    }
    adjSet.put(u, v);
    adjSet.put(v, u);
  }

  private boolean isMoveStmt(Stmt stmt) {
    return switch (stmt.getStmtType()) {
      case VAR_ASSIGN -> {
        final var vas = (VarAssignStmt) stmt;
        yield switch (vas.rhs().getExprType()) {
          case RVAL -> vas.rhs() instanceof IdRvalExpr ire && ire.id().equals(vas.lhs().id());
          case NEW, CALL -> true;
          case BINARY, FIELD, UNARY -> false;
        };
      }
      case CALL, READLN, PRINTLN, RETURN -> true;
      case GOTO, CMP, LABEL, FIELD_ASSIGN -> false;
    };
  }
}
