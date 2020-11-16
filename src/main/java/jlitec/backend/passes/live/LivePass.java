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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.flow.FlowGraph;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAssignLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.PushStackLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public class LivePass implements Pass<MethodWithFlow, Method> {
  public record DefUse(Set<Node> use, Set<Node> def) {
    public static DefUse EMPTY = new DefUse(Set.of(), Set.of());

    public DefUse {
      this.use = Collections.unmodifiableSet(use);
      this.def = Collections.unmodifiableSet(def);
    }

    public static DefUse combine(DefUse defUse1, DefUse defUse2) {
      return new DefUse(Sets.union(defUse1.use, defUse2.use), Sets.union(defUse1.def, defUse2.def));
    }
  }

  private record InOut(SetMultimap<Integer, Node> in, SetMultimap<Integer, Node> out) {
    public InOut {
      this.in = Multimaps.unmodifiableSetMultimap(in);
      this.out = Multimaps.unmodifiableSetMultimap(out);
    }
  }

  @Override
  public Method pass(MethodWithFlow input) {
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

    return new Method(
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
      case LABEL, GOTO, RETURN, POP_STACK -> DefUse.EMPTY;
      case BINARY -> {
        final var bs = (BinaryLowerStmt) stmt;
        yield new DefUse(
            Set.of(new Node.Id(bs.lhs()), new Node.Id(bs.rhs())), Set.of(new Node.Id(bs.dest())));
      }
      case BRANCH_LINK -> {
        final Set<Node> paramRegNodes =
            IntStream.range(0, 4)
                .boxed()
                .map(Register::fromInt)
                .map(Node.Reg::new)
                .collect(Collectors.toUnmodifiableSet());
        yield new DefUse(paramRegNodes, paramRegNodes);
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
        yield new DefUse(Set.of(new Node.Id(fas.lhsId()), new Node.Id(fas.rhs())), Set.of());
      }
      case MOV -> {
        final var ms = (MovLowerStmt) stmt;
        final var use =
            Stream.of(ms.src())
                .map(LivePass::toNode)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
        final var def =
            Stream.of(ms.dst())
                .map(LivePass::toNode)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
        yield new DefUse(use, def);
      }
      case PUSH_STACK -> {
        final var pss = (PushStackLowerStmt) stmt;
        final Set<Node> use =
            pss.elements().stream()
                .map(IdRvalExpr::id)
                .map(Node.Id::new)
                .collect(Collectors.toUnmodifiableSet());
        yield new DefUse(use, Set.of());
      }
      case UNARY -> {
        final var us = (UnaryLowerStmt) stmt;
        yield new DefUse(Set.of(new Node.Id(us.expr())), Set.of(new Node.Id(us.dest())));
      }
    };
  }

  private static Optional<Node> toNode(Addressable addressable) {
    return switch (addressable.type()) {
      case RVAL -> {
        final var a = (Addressable.Rval) addressable;
        yield switch (a.rvalExpr().getRvalExprType()) {
          case ID -> {
            final var idRvalExpr = (IdRvalExpr) a.rvalExpr();
            yield Optional.of(new Node.Id(idRvalExpr.id()));
          }
          case STRING, NULL, INT, BOOL -> Optional.empty();
        };
      }
      case REG -> {
        final var a = (Addressable.Reg) addressable;
        yield Optional.of(new Node.Reg(a.reg()));
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
