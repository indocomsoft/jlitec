package jlitec.backend.passes.regalloc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.flow.FlowGraph;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.live.MethodWithLive;
import jlitec.backend.passes.live.Node;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.ir3.Var;

public class RegAllocPass implements Pass<jlitec.backend.passes.lower.Method, RegAllocPass.Output> {
  private static int NUM_REG = 13;
  private static Set<Node> precolored =
      IntStream.range(0, 13)
          .boxed()
          .map(Register::fromInt)
          .map(Node.Reg::new)
          .collect(Collectors.toUnmodifiableSet());

  public static record Output(Map<String, Register> color, Method method) {
    public Output {
      this.color = Collections.unmodifiableMap(color);
    }
  }

  private FlowGraph flowGraph;
  private MethodWithLive methodWithLive;

  /* Node work lists, sets, and stacks */
  private Set<Node> simplifyWorklist;
  private Set<Node> freezeWorklist;
  private Set<Node> spillWorklist;
  private Set<Node> spilledNodes;
  private Set<Node> coalescedNodes;
  private Set<Node> coloredNodes;
  private Stack<Node> selectStack;

  /* Move sets */
  private Set<MovLowerStmt> coalescedMoves;
  private Set<MovLowerStmt> constrainedMoves;
  private Set<MovLowerStmt> frozenMoves;
  private Set<MovLowerStmt> worklistMoves;
  private Set<MovLowerStmt> activeMoves;

  /* Other data structures */
  private SetMultimap<Node, Node> adjList;
  private SetMultimap<Node, Node> adjSet;
  private Map<Node, Integer> degree;
  private SetMultimap<Node, MovLowerStmt> moveList;
  private Map<Node, Node> alias;
  private Map<Node, Integer> color;

  @Override
  public Output pass(Method input) {
    var method = input;
    while (true) {
      // initialize Node work lists, sets, and stacks
      simplifyWorklist = new HashSet<>();
      freezeWorklist = new HashSet<>();
      spillWorklist = new HashSet<>();
      spilledNodes = new HashSet<>();
      coalescedNodes = new HashSet<>();
      coloredNodes = new HashSet<>();
      selectStack = new Stack<>();

      // initialize move sets
      coalescedMoves = new HashSet<>();
      constrainedMoves = new HashSet<>();
      frozenMoves = new HashSet<>();
      worklistMoves = new HashSet<>();
      activeMoves = new HashSet<>();

      // Initialize Other data structures.
      adjList = HashMultimap.create();
      adjSet = HashMultimap.create();
      degree =
          new HashMap<>(precolored.stream().collect(Collectors.toMap(Function.identity(), n -> 0)));
      moveList = HashMultimap.create();
      alias = new HashMap<>();
      color =
          new HashMap<>(
              IntStream.range(0, NUM_REG)
                  .boxed()
                  .collect(
                      Collectors.toMap(
                          i -> new Node.Reg(Register.fromInt(i)), Function.identity())));

      // LivenessAnalysis()
      flowGraph = FlowPass.process(method.lowerStmtList());
      methodWithLive = new LivePass().pass(new MethodWithFlow(method, flowGraph));

      // Build()
      build();

      // MakeWorklist()
      final var initial =
          Stream.concat(method.argsWithThis().stream(), method.vars().stream())
              .map(Var::id)
              .map(Node.Id::new)
              .collect(Collectors.toUnmodifiableSet());
      makeWorklist(initial);

      do {
        if (!simplifyWorklist.isEmpty()) {
          simplify();
        } else if (!worklistMoves.isEmpty()) {
          coalesce();
        } else if (!freezeWorklist.isEmpty()) {
          freeze();
        } else if (!spillWorklist.isEmpty()) {
          selectSpill(method);
        }
      } while (!simplifyWorklist.isEmpty()
          || !worklistMoves.isEmpty()
          || !freezeWorklist.isEmpty()
          || !spillWorklist.isEmpty());
      assignColors();
      if (!spilledNodes.isEmpty()) {
        throw new RuntimeException("Unimplemented");
        // TODO, also set the new method
        //        rewriteProgram(spilledNodes);
        //        continue;
      }
      break;
    }

    System.out.println(method.id());
    System.out.println(color);

    return null;
  }

  private void build() {
    for (final var blockWithLive : methodWithLive.blockWithLiveList()) {
      final var block = blockWithLive.block();
      if (block.type() == Block.Type.EXIT) continue;
      final var b = (Block.Basic) block;
      final var live = new HashSet<>(blockWithLive.liveOut());
      for (final var stmt : Lists.reverse(b.lowerStmtList())) {
        final var defUse = LivePass.calculateDefUse(stmt);
        if (stmt instanceof MovLowerStmt ms) {
          live.removeAll(defUse.use());
          for (final var n : Sets.union(defUse.def(), defUse.use())) {
            moveList.put(n, ms);
          }
          worklistMoves.add(ms);
        }
        live.addAll(defUse.def());
        for (final var d : defUse.def()) {
          for (final var l : live) {
            addEdge(l, d);
          }
        }
        live.removeAll(defUse.def());
        live.addAll(defUse.use());
      }
    }
  }

  private void addEdge(Node u, Node v) {
    if (adjSet.containsEntry(u, v) || u.equals(v)) {
      return;
    }
    adjSet.put(u, v);
    adjSet.put(v, u);
    if (!precolored.contains(u)) {
      adjList.put(u, v);
      degree.put(u, degree.getOrDefault(u, 0) + 1);
    }
    if (!precolored.contains(v)) {
      adjList.put(v, u);
      degree.put(v, degree.getOrDefault(v, 0) + 1);
    }
  }

  private void makeWorklist(Set<Node.Id> initial) {
    for (final var n : initial) {
      if (degree.get(n) >= NUM_REG) {
        spillWorklist.add(n);
      } else if (moveRelated(n)) {
        freezeWorklist.add(n);
      } else {
        simplifyWorklist.add(n);
      }
    }
  }

  private Set<Node> adjacent(Node n) {
    return Sets.difference(adjList.get(n), Sets.union(Set.copyOf(selectStack), coalescedNodes));
  }

  private boolean moveRelated(Node n) {
    return !nodeMoves(n).isEmpty();
  }

  private Set<MovLowerStmt> nodeMoves(Node n) {
    return Sets.intersection(moveList.get(n), Sets.union(activeMoves, worklistMoves));
  }

  private void simplify() {
    final var n = Iterables.get(simplifyWorklist, 0);
    simplifyWorklist.remove(n);
    selectStack.push(n);
    for (final var m : adjacent(n)) {
      decrementDegree(m);
    }
  }

  private void decrementDegree(Node m) {
    final var d = degree.get(m);
    degree.put(m, d - 1);
    if (d == NUM_REG) {
      enableMoves(Sets.union(Set.of(m), adjacent(m)));
      spillWorklist.remove(m);
      if (moveRelated(m)) {
        freezeWorklist.add(m);
      } else {
        simplifyWorklist.add(m);
      }
    }
  }

  private void enableMoves(Set<Node> nodes) {
    for (final var n : nodes) {
      for (final var m : nodeMoves(n)) {
        if (activeMoves.contains(m)) {
          activeMoves.remove(m);
          worklistMoves.add(m);
        }
      }
    }
  }

  private void addWorkList(Node u) {
    if (u.type() != Node.Type.REG && !moveRelated(u) && degree.get(u) < NUM_REG) {
      freezeWorklist.remove(u);
      simplifyWorklist.add(u);
    }
  }

  private boolean ok(Node t, Node r) {
    return degree.get(t) < NUM_REG || t.type() == Node.Type.REG || adjSet.containsEntry(t, r);
  }

  private boolean conservative(Set<Node> nodes) {
    return nodes.stream().filter(n -> degree.get(n) >= NUM_REG).count() < NUM_REG;
  }

  private void coalesce() {
    final var m = Iterables.get(worklistMoves, 0);
    final var x = getAlias(m.dst().toNode());
    final var y = getAlias(m.src().toNode());
    final Node u, v;
    if (y.type() == Node.Type.REG) {
      u = y;
      v = x;
    } else {
      u = x;
      v = y;
    }
    worklistMoves.remove(m);
    if (u.equals(v)) {
      coalescedMoves.add(m);
      addWorkList(u);
    } else if (v.type() == Node.Type.REG || adjSet.containsEntry(u, v)) {
      constrainedMoves.add(m);
      addWorkList(u);
      addWorkList(v);
    } else if ((u.type() == Node.Type.REG && adjacent(v).stream().allMatch(t -> ok(t, u)))
        || (u.type() != Node.Type.REG && conservative(Sets.union(adjacent(u), adjacent(v))))) {
      coalescedMoves.add(m);
      combine(u, v);
      addWorkList(u);
    } else {
      activeMoves.add(m);
    }
  }

  private void combine(Node u, Node v) {
    if (freezeWorklist.contains(v)) {
      freezeWorklist.remove(v);
    } else {
      spillWorklist.remove(v);
    }
    coalescedNodes.add(v);
    alias.put(v, u);
    moveList.putAll(u, Sets.union(moveList.get(u), moveList.get(v)));
    enableMoves(Set.of(v));
    for (final var t : adjacent(v)) {
      addEdge(t, u);
      decrementDegree(t);
    }
    if (degree.get(u) >= NUM_REG && freezeWorklist.contains(u)) {
      freezeWorklist.remove(u);
      spillWorklist.add(u);
    }
  }

  private Node getAlias(Node n) {
    if (coalescedNodes.contains(n)) {
      return getAlias(alias.get(n));
    } else {
      return n;
    }
  }

  private void freeze() {
    final var u = Iterables.get(freezeWorklist, 0);
    freezeWorklist.remove(u);
    simplifyWorklist.add(u);
    freezeMoves(u);
  }

  private void freezeMoves(Node u) {
    for (final var m : nodeMoves(u)) {
      final var x = m.dst().toNode();
      final var y = m.src().toNode();
      final var v = getAlias(y).equals(getAlias(u)) ? getAlias(x) : getAlias(y);
      activeMoves.remove(m);
      frozenMoves.add(m);
      if (freezeWorklist.contains(v) && nodeMoves(v).isEmpty()) {
        freezeWorklist.remove(v);
        simplifyWorklist.add(v);
      }
    }
  }

  private Map<Node.Id, Double> calculateSpillPriority(Method method) {
    final Map<Node.Id, Integer> counter = new HashMap<>();
    for (final var stmt : method.lowerStmtList()) {
      final var defUse = LivePass.calculateDefUse(stmt);
      for (final var n : defUse.def()) {
        if (n instanceof Node.Id nid) {
          counter.put(nid, counter.getOrDefault(nid, 0) + 1);
        }
      }
      for (final var n : defUse.use()) {
        if (n instanceof Node.Id nid) {
          counter.put(nid, counter.getOrDefault(nid, 0) + 1);
        }
      }
    }
    return counter.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, e -> (double) e.getValue() / (double) degree.get(e.getKey())));
  }

  private void selectSpill(Method method) {
    final var spillPriorityScore = calculateSpillPriority(method);
    final var m = spillWorklist.stream().min(Comparator.comparing(spillPriorityScore::get)).get();
    spillWorklist.remove(m);
    simplifyWorklist.add(m);
    freezeMoves(m);
  }

  private void assignColors() {
    while (!selectStack.isEmpty()) {
      final var n = selectStack.pop();
      final var okColors =
          new HashSet<>(
              IntStream.range(0, NUM_REG).boxed().collect(Collectors.toUnmodifiableSet()));
      for (final var w : adjList.get(n)) {
        if (Sets.union(coloredNodes, precolored).contains(getAlias(w))) {
          okColors.remove(color.get(getAlias(w)));
        }
      }
      if (okColors.isEmpty()) {
        spilledNodes.add(n);
      } else {
        coloredNodes.add(n);
        final var c = Iterables.get(okColors, 0);
        color.put(n, c);
      }
    }
    for (final var n : coalescedNodes) {
      color.put(n, color.get(getAlias(n)));
    }
  }
}
