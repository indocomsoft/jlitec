package jlitec.backend.passes.optimization.constantfolding;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BinaryWithBitLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LabelLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadLargeImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadStackArgLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractWithBitLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;

public class ReachingPass implements Pass<Method, ReachingPass.InOut> {
  private static record GenKill(Set<Integer> gen, Set<Integer> kill) {
    public static final GenKill EMPTY = new GenKill(Set.of(), Set.of());

    public GenKill {
      gen = Collections.unmodifiableSet(gen);
      kill = Collections.unmodifiableSet(kill);
    }

    public static GenKill combine(GenKill genKill1, GenKill genKill2) {
      return new GenKill(
          Sets.union(genKill1.gen, genKill2.gen), Sets.union(genKill1.kill, genKill2.kill));
    }
  }

  @Override
  public InOut pass(Method input) {
    final var stmtWithGenKillList = genStmtWithGenKillList(input.lowerStmtList());
    final var edges = buildEdges(input.lowerStmtList());
    final var inOut = dataflow(stmtWithGenKillList, edges);
    return inOut;
  }

  /** @return -1 is a pointer to exit of the program */
  private static SetMultimap<Integer, Integer> buildEdges(List<LowerStmt> stmtList) {
    final var labelToIndex =
        IntStream.range(0, stmtList.size())
            .filter(i -> stmtList.get(i) instanceof LabelLowerStmt)
            .boxed()
            .collect(
                Collectors.toUnmodifiableMap(
                    i -> ((LabelLowerStmt) stmtList.get(i)).label(), Function.identity()));
    final var result = HashMultimap.<Integer, Integer>create();
    for (int i = 0; i < stmtList.size(); i++) {
      final var stmt = stmtList.get(i);
      switch (stmt.stmtExtensionType()) {
        case CMP -> {
          final var cs = (CmpLowerStmt) stmt;
          result.put(i, labelToIndex.get(cs.dest()));
          if (i + 1 < stmtList.size()) {
            result.put(i, i + 1);
          }
        }
        case GOTO -> {
          final var gs = (GotoLowerStmt) stmt;
          result.put(i, labelToIndex.get(gs.dest()));
        }
        case RETURN -> result.put(i, -1);
        default -> {
          if (i + 1 < stmtList.size()) {
            result.put(i, i + 1);
          }
        }
      }
      ;
    }
    return Multimaps.unmodifiableSetMultimap(result);
  }

  // mapping from stmt index number to the reaching definitions
  public static record InOut(SetMultimap<Integer, Integer> in, SetMultimap<Integer, Integer> out) {
    public InOut {
      in = Multimaps.unmodifiableSetMultimap(in);
      out = Multimaps.unmodifiableSetMultimap(out);
    }
  }

  private static InOut dataflow(
      List<StmtWithGenKill> stmtWithGenKillList, SetMultimap<Integer, Integer> edges) {
    final var predecessors = Multimaps.invertFrom(edges, HashMultimap.create());
    final var in = HashMultimap.<Integer, Integer>create();
    final var out = HashMultimap.<Integer, Integer>create();

    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < stmtWithGenKillList.size(); i++) {
        final var stmtWithGenKill = stmtWithGenKillList.get(i);
        final var stmt = stmtWithGenKill.stmt;
        final var genKill = stmtWithGenKill.genKill;
        final var gen = genKill.gen;
        final var kill = genKill.kill;

        // IN[B] = U_{P a predecessor of B} OUT[P];
        final var newIn = new HashSet<Integer>();
        for (final var predecessor : predecessors.get(i)) {
          newIn.addAll(out.get(predecessor));
        }
        in.replaceValues(i, newIn);

        // OUT[B] = gen_B U (IN[B] - kill_B)
        final var newOut = Sets.union(gen, Sets.difference(in.get(i), kill));
        changed = !newOut.equals(out.get(i));
        out.replaceValues(i, newOut);
      }
    }
    return new InOut(in, out);
  }

  private static record StmtWithGenKill(LowerStmt stmt, GenKill genKill) {}

  private List<StmtWithGenKill> genStmtWithGenKillList(List<LowerStmt> stmtList) {
    final var defs = buildDefs(stmtList);

    final var result = new ArrayList<StmtWithGenKill>();
    for (int i = 0; i < stmtList.size(); i++) {
      final var stmt = stmtList.get(i);
      final var genKill = calculateGenKill(stmt, i, defs);
      result.add(new StmtWithGenKill(stmt, genKill));
    }
    return Collections.unmodifiableList(result);
  }

  private static SetMultimap<Node, Integer> buildDefs(List<LowerStmt> stmtList) {
    final var result = HashMultimap.<Node, Integer>create();
    for (int i = 0; i < stmtList.size(); i++) {
      final var stmt = stmtList.get(i);
      final var def = nodesOf(stmt);
      if (def.isEmpty()) {
        continue;
      }
      for (final var node : def) {
        result.put(node, i);
      }
    }
    return Multimaps.unmodifiableSetMultimap(result);
  }

  private static GenKill calculateGenKill(
      LowerStmt stmt, int index, SetMultimap<Node, Integer> defs) {
    final var nodes = nodesOf(stmt);
    if (nodes.isEmpty()) {
      return GenKill.EMPTY;
    }
    final var gen = Set.of(index);
    final var kill =
        nodes.stream()
            .flatMap(n -> defs.get(n).stream())
            .filter(i -> i != index)
            .collect(Collectors.toUnmodifiableSet());
    return new GenKill(gen, kill);
  }

  public static Set<Node> nodesOf(LowerStmt stmt) {
    return switch (stmt.stmtExtensionType()) {
      case CMP, GOTO, LABEL, STR_SPILL, RETURN, PUSH_PAD_STACK, PUSH_STACK, POP_STACK, FIELD_ASSIGN -> Set
          .of();
      case BRANCH_LINK -> IntStream.range(0, 4)
          .boxed()
          .map(Register::fromInt)
          .map(Node.Reg::new)
          .collect(Collectors.toUnmodifiableSet());
      case BINARY_BIT -> {
        final var bbs = (BinaryWithBitLowerStmt) stmt;
        yield Set.of(bbs.dest().toNode());
      }
      case BINARY -> {
        final var bs = (BinaryLowerStmt) stmt;
        yield Set.of(bs.dest().toNode());
      }
      case REVERSE_SUBTRACT_BIT -> {
        final var rsbs = (ReverseSubtractWithBitLowerStmt) stmt;
        yield Set.of(rsbs.dest().toNode());
      }
      case REVERSE_SUBTRACT -> {
        final var rss = (ReverseSubtractLowerStmt) stmt;
        yield Set.of(rss.dest().toNode());
      }
      case BIT -> {
        final var bs = (BitLowerStmt) stmt;
        yield Set.of(new Node.Id(bs.dest()));
      }
      case FIELD_ACCESS -> {
        final var fas = (FieldAccessLowerStmt) stmt;
        yield Set.of(new Node.Id(fas.lhs()));
      }
      case LOAD_LARGE_IMM -> {
        final var is = (LoadLargeImmediateLowerStmt) stmt;
        yield Set.of(is.dest().toNode());
      }
      case IMMEDIATE -> {
        final var is = (ImmediateLowerStmt) stmt;
        yield Set.of(is.dest().toNode());
      }
      case LOAD_STACK_ARG -> {
        final var lsas = (LoadStackArgLowerStmt) stmt;
        yield Set.of(new Node.Id(lsas.stackArg().id()));
      }
      case LDR_SPILL -> {
        final var lss = (LoadSpilledLowerStmt) stmt;
        yield Set.of(new Node.Id(lss.dst()));
      }
      case MOV -> {
        final var ms = (MovLowerStmt) stmt;
        yield Set.of(ms.dst().toNode());
      }
      case REG_BINARY -> {
        final var rbs = (RegBinaryLowerStmt) stmt;
        yield Set.of(rbs.dest().toNode());
      }
      case UNARY -> {
        final var us = (UnaryLowerStmt) stmt;
        yield Set.of(new Node.Id(us.dest()));
      }
    };
  }
}
