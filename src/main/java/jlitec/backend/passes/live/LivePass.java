package jlitec.backend.passes.live;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.flow.FlowGraph;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BinaryWithBitLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAssignLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadLargeImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadStackArgLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.PushStackLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractWithBitLowerStmt;
import jlitec.backend.passes.lower.stmt.StoreSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public class LivePass implements Pass<MethodWithFlow, MethodWithLive> {
  public record DefUse(Set<Node> use, Set<Node> def) {
    public static final DefUse EMPTY = new DefUse(Set.of(), Set.of());

    public DefUse {
      use = Collections.unmodifiableSet(use);
      def = Collections.unmodifiableSet(def);
    }

    public static DefUse combine(DefUse defUse1, DefUse defUse2) {
      return new DefUse(Sets.union(defUse1.use, defUse2.use), Sets.union(defUse1.def, defUse2.def));
    }
  }

  private record InOut(SetMultimap<Integer, Node> in, SetMultimap<Integer, Node> out) {
    public InOut {
      in = Multimaps.unmodifiableSetMultimap(in);
      out = Multimaps.unmodifiableSetMultimap(out);
    }
  }

  @Override
  public MethodWithLive pass(MethodWithFlow input) {
    final var defUseList =
        input.flowGraph().blocks().stream()
            .map(LivePass::calculateDefUse)
            .collect(Collectors.toUnmodifiableList());
    final var inOut = dataflow(defUseList, input.flowGraph());
    final var blockWithLiveList =
        IntStream.range(0, input.flowGraph().blocks().size())
            .boxed()
            .map(
                i -> {
                  final var block = input.flowGraph().blocks().get(i);
                  final var in = inOut.in.get(i);
                  final var out = inOut.out.get(i);
                  return new BlockWithLive(block, in, out);
                })
            .collect(Collectors.toUnmodifiableList());

    final var stmtWithLiveList = calculateStmtLive(blockWithLiveList);

    return new MethodWithLive(
        input.method().returnType(),
        input.method().id(),
        input.method().argsWithThis(),
        input.method().vars(),
        blockWithLiveList,
        stmtWithLiveList);
  }

  private List<LowerStmtWithLive> calculateStmtLive(List<BlockWithLive> blockWithLiveList) {
    final var result = new ArrayList<LowerStmtWithLive>();

    for (final var blockWithLive : blockWithLiveList) {
      final List<LowerStmtWithLive> stmtList =
          switch (blockWithLive.block().type()) {
            case EXIT -> List.of();
            case BASIC -> {
              final var bb = (Block.Basic) blockWithLive.block();
              final var reversedBlockStmt = new ArrayList<LowerStmtWithLive>();
              Set<Node> currentLiveOut = blockWithLive.liveOut();
              for (final var lowerStmt : Lists.reverse(bb.lowerStmtList())) {
                // OUT[S] = U_{S a successor of B] IN[S]
                // And the only successor is the statement after (or live out of the block)
                final var liveOut = currentLiveOut;
                // IN[S] = (OUT[B] - def_B) U use_B
                final var liveIn = new HashSet<Node>();
                liveIn.addAll(currentLiveOut);
                final var stmtDefUse = calculateDefUse(lowerStmt);
                liveIn.removeAll(stmtDefUse.def());
                liveIn.addAll(stmtDefUse.use());
                reversedBlockStmt.add(new LowerStmtWithLive(lowerStmt, liveIn, liveOut));
                currentLiveOut = Collections.unmodifiableSet(liveIn);
              }
              yield Lists.reverse(reversedBlockStmt);
            }
          };
      result.addAll(stmtList);
    }

    return Collections.unmodifiableList(result);
  }

  private InOut dataflow(List<DefUse> defUseList, FlowGraph flowGraph) {
    final var in = HashMultimap.<Integer, Node>create();
    final var out = HashMultimap.<Integer, Node>create();
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < flowGraph.blocks().size(); i++) {
        final var block = flowGraph.blocks().get(i);
        final var defUse = defUseList.get(i);
        final var def = defUse.def();
        final var use = defUse.use();

        // OUT[B] = U_{S a successor of B} IN[S];
        final var newOut = new HashSet<Node>();
        for (final var j : flowGraph.edges().get(i)) {
          newOut.addAll(in.get(j));
        }
        changed = changed || !newOut.equals(out.get(i));
        out.replaceValues(i, newOut);

        // IN[B] = use_B U (OUT[B] - def_B)
        final var newIn = Sets.union(use, Sets.difference(out.get(i), def));
        changed = changed || !newIn.equals(in.get(i));
        in.replaceValues(i, newIn);
      }
    }
    return new InOut(in, out);
  }

  private static DefUse calculateDefUse(Block block) {
    return switch (block.type()) {
      case EXIT -> DefUse.EMPTY;
      case BASIC -> {
        final var bb = (Block.Basic) block;
        final var use = new HashSet<Node>();
        final var def = new HashSet<Node>();
        for (final var stmt : Lists.reverse(bb.lowerStmtList())) {
          final var defUse = calculateDefUse(stmt);
          use.removeAll(defUse.def());
          use.addAll(defUse.use());
          def.addAll(defUse.def());
        }
        yield new DefUse(use, def);
      }
    };
  }

  public static DefUse calculateDefUse(LowerStmt stmt) {
    return switch (stmt.stmtExtensionType()) {
      case LOAD_STACK_ARG -> {
        final var lsas = (LoadStackArgLowerStmt) stmt;
        yield new DefUse(Set.of(), Set.of(new Node.Id(lsas.stackArg().id())));
      }
      case IMMEDIATE -> {
        final var is = (ImmediateLowerStmt) stmt;
        yield new DefUse(Set.of(), Set.of(toNode(is.dest())));
      }
      case LOAD_LARGE_IMM -> {
        final var is = (LoadLargeImmediateLowerStmt) stmt;
        yield new DefUse(Set.of(), Set.of(toNode(is.dest())));
      }
      case LABEL, GOTO, POP_STACK, PUSH_PAD_STACK -> DefUse.EMPTY;
      case RETURN -> new DefUse(Set.of(new Node.Reg(Register.R0)), Set.of());
      case BINARY_BIT -> {
        final var bbs = (BinaryWithBitLowerStmt) stmt;
        final var use = new HashSet<Node>();
        use.add(bbs.lhs().toNode());
        use.add(new Node.Id(bbs.rhs()));
        yield new DefUse(use, Set.of(bbs.dest().toNode()));
      }
      case BINARY -> {
        final var bs = (BinaryLowerStmt) stmt;
        final var use = new HashSet<Node>();
        use.add(bs.lhs().toNode());
        if (bs.rhs() instanceof IdRvalExpr ire) {
          use.add(new Node.Id(ire));
        }
        yield new DefUse(use, Set.of(bs.dest().toNode()));
      }
      case REVERSE_SUBTRACT_BIT -> {
        final var rsbs = (ReverseSubtractWithBitLowerStmt) stmt;
        final var use = new HashSet<Node>();
        use.add(rsbs.lhs().toNode());
        use.add(new Node.Id(rsbs.rhs()));
        yield new DefUse(use, Set.of(rsbs.dest().toNode()));
      }
      case REVERSE_SUBTRACT -> {
        final var rss = (ReverseSubtractLowerStmt) stmt;
        final var use = new HashSet<Node>();
        use.add(rss.lhs().toNode());
        if (rss.rhs() instanceof IdRvalExpr ire) {
          use.add(new Node.Id(ire));
        }
        yield new DefUse(use, Set.of(rss.dest().toNode()));
      }
      case REG_BINARY -> {
        final var bs = (RegBinaryLowerStmt) stmt;
        final Set<Node> use =
            Stream.of(bs.lhs(), bs.rhs())
                .map(Addressable::toNode)
                .collect(Collectors.toUnmodifiableSet());
        yield new DefUse(use, Set.of(bs.dest().toNode()));
      }
      case BRANCH_LINK -> {
        final Set<Node> use =
            IntStream.range(0, 4)
                .boxed()
                .map(Register::fromInt)
                .map(Node.Reg::new)
                .collect(Collectors.toUnmodifiableSet());
        final Set<Node> def =
            Stream.of(Register.R0, Register.R1, Register.R2, Register.R3, Register.R12, Register.LR)
                .map(Node.Reg::new)
                .collect(Collectors.toUnmodifiableSet());
        yield new DefUse(use, def);
      }
      case CMP -> {
        final var cs = (CmpLowerStmt) stmt;
        yield DefUse.combine(calculateDefUse(cs.lhs()), calculateDefUse(cs.rhs()));
      }
      case FIELD_ACCESS -> {
        final var fas = (FieldAccessLowerStmt) stmt;
        yield new DefUse(Set.of(new Node.Id(fas.rhsId())), Set.of(new Node.Id(fas.lhs())));
      }
      case FIELD_ASSIGN -> {
        final var fas = (FieldAssignLowerStmt) stmt;
        final Set<Node> use =
            Stream.of(new Node.Id(fas.lhsId()), fas.rhs().toNode())
                .collect(Collectors.toUnmodifiableSet());
        yield new DefUse(use, Set.of());
      }
      case LDR_SPILL -> {
        final var lss = (LoadSpilledLowerStmt) stmt;
        yield new DefUse(Set.of(), Set.of(new Node.Id(lss.dst())));
      }
      case STR_SPILL -> {
        final var sss = (StoreSpilledLowerStmt) stmt;
        yield new DefUse(Set.of(new Node.Id(sss.src())), Set.of());
      }
      case MOV -> {
        final var ms = (MovLowerStmt) stmt;
        final var use = Set.of(toNode(ms.src()));
        final var def = Set.of(toNode(ms.dst()));
        yield new DefUse(use, def);
      }
      case PUSH_STACK -> {
        final var pss = (PushStackLowerStmt) stmt;
        yield new DefUse(Set.of(new Node.Id(pss.idRvalExpr())), Set.of());
      }
      case UNARY -> {
        final var us = (UnaryLowerStmt) stmt;
        yield new DefUse(Set.of(new Node.Id(us.expr())), Set.of(new Node.Id(us.dest())));
      }
      case BIT -> {
        final var bs = (BitLowerStmt) stmt;
        yield new DefUse(Set.of(new Node.Id(bs.expr())), Set.of(new Node.Id(bs.dest())));
      }
    };
  }

  private static Node toNode(Addressable addressable) {
    return switch (addressable.type()) {
      case ID_RVAL -> {
        final var a = (Addressable.IdRval) addressable;
        yield new Node.Id(a.idRvalExpr());
      }
      case REG -> {
        final var a = (Addressable.Reg) addressable;
        yield new Node.Reg(a.reg());
      }
    };
  }

  private static DefUse calculateDefUse(RvalExpr expr) {
    return switch (expr.getRvalExprType()) {
      case ID -> {
        final var ire = (IdRvalExpr) expr;
        yield new DefUse(Set.of(new Node.Id(ire.id())), Set.of());
      }
      case STRING, NULL, INT, BOOL -> DefUse.EMPTY;
    };
  }
}
