package jlitec.backend.passes.regalloc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.flow.FlowGraph;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.live.MethodWithLive;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAccessLowerStmt;
import jlitec.backend.passes.lower.stmt.FieldAssignLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.LoadStackArgLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.PushStackLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractLowerStmt;
import jlitec.backend.passes.lower.stmt.StoreSpilledLowerStmt;
import jlitec.backend.passes.lower.stmt.UnaryLowerStmt;
import jlitec.ir3.Var;
import jlitec.ir3.codegen.TempVarGen;
import jlitec.ir3.expr.rval.IdRvalExpr;

public class RegAllocPass implements Pass<jlitec.backend.passes.lower.Method, RegAllocPass.Output> {
  private static final int NUM_REG = 13;
  private static final Set<Node> precolored =
      IntStream.range(0, NUM_REG)
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
  private Set<Node.Id> simplifyWorklist;
  private Set<Node.Id> freezeWorklist;
  private Set<Node.Id> spillWorklist;
  private Set<Node.Id> spilledNodes;
  private Set<Node.Id> coalescedNodes;
  private Set<Node.Id> coloredNodes;
  private Stack<Node.Id> selectStack;

  /* Move sets */
  private Set<MovLowerStmt> coalescedMoves;
  private Set<MovLowerStmt> constrainedMoves;
  private Set<MovLowerStmt> frozenMoves;
  private Set<MovLowerStmt> worklistMoves;
  private Set<MovLowerStmt> activeMoves;

  /* Other data structures */
  private SetMultimap<Node.Id, Node> adjList;
  private SetMultimap<Node, Node> adjSet;
  private Map<Node, Integer> degree;
  private SetMultimap<Node, MovLowerStmt> moveList;
  private Map<Node.Id, Node> alias;
  private Map<Node, Integer> color;

  @Override
  public Output pass(Method input) {
    var method = input;
    while (true) {
      final var nodeDefUse = calculateNodeDefUse(method);
      final var initial =
          Stream.concat(method.argsWithThis().stream(), method.vars().stream())
              .map(Var::id)
              .map(Node.Id::new)
              .collect(Collectors.toUnmodifiableSet());
      final Set<String> stackArgIdList =
          method.argsWithThis().size() > 4
              ? method.argsWithThis().subList(4, method.argsWithThis().size()).stream()
                  .map(Var::id)
                  .collect(Collectors.toUnmodifiableSet())
              : Set.of();

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
          new HashMap<>(
              Stream.concat(precolored.stream(), initial.stream())
                  .collect(Collectors.toMap(Function.identity(), n -> 0)));
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
      flowGraph = new FlowPass().pass(method.lowerStmtList());
      methodWithLive = new LivePass().pass(new MethodWithFlow(method, flowGraph));

      // Build()
      build();

      // MakeWorklist()
      makeWorklist(initial);

      do {
        if (!simplifyWorklist.isEmpty()) {
          simplify();
        } else if (!worklistMoves.isEmpty()) {
          coalesce();
        } else if (!freezeWorklist.isEmpty()) {
          freeze();
        } else if (!spillWorklist.isEmpty()) {
          selectSpill(nodeDefUse, stackArgIdList);
        }
      } while (!simplifyWorklist.isEmpty()
          || !worklistMoves.isEmpty()
          || !freezeWorklist.isEmpty()
          || !spillWorklist.isEmpty());
      assignColors();
      if (!spilledNodes.isEmpty()) {
        method = rewriteProgram(method);
        continue;
      }
      break;
    }

    final var outputColor =
        color.entrySet().stream()
            .filter(e -> e.getKey() instanceof Node.Id)
            .collect(
                Collectors.toUnmodifiableMap(
                    e -> ((Node.Id) e.getKey()).id(), e -> Register.fromInt(e.getValue())));
    return new Output(outputColor, method);
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
    if (u instanceof Node.Id uid) {
      adjList.put(uid, v);
      degree.put(uid, degree.get(uid) + 1);
    }
    if (v instanceof Node.Id vid) {
      adjList.put(vid, u);
      degree.put(vid, degree.get(vid) + 1);
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

  private Set<Node> adjacent(Node.Id n) {
    return Sets.difference(adjList.get(n), Sets.union(Set.copyOf(selectStack), coalescedNodes));
  }

  private boolean moveRelated(Node.Id n) {
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
      if (m instanceof Node.Id mid) {
        decrementDegree(mid);
      }
    }
  }

  private void decrementDegree(Node.Id m) {
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
    if (u instanceof Node.Id uid && !moveRelated(uid) && degree.get(uid) < NUM_REG) {
      freezeWorklist.remove(uid);
      simplifyWorklist.add(uid);
    }
  }

  private boolean ok(Node t, Node.Reg r) {
    return degree.get(t) < NUM_REG || t instanceof Node.Reg || adjSet.containsEntry(t, r);
  }

  private boolean conservative(Set<Node> nodes) {
    return nodes.stream().filter(n -> degree.get(n) >= NUM_REG).count() < NUM_REG;
  }

  private void coalesce() {
    final var m = Iterables.get(worklistMoves, 0);
    final var x = getAlias(m.dst().toNode());
    final var y = getAlias(m.src().toNode());
    final Node u, v;
    // Either u is Node.Reg or v is Node.Id
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
    } else {
      // From the previous check, v is not Node.Reg => v is Node.Id
      final var vid = (Node.Id) v;
      if ((u instanceof Node.Reg ureg && adjacent(vid).stream().allMatch(t -> ok(t, ureg)))
          || (u instanceof Node.Id uid && conservative(Sets.union(adjacent(uid), adjacent(vid))))) {
        coalescedMoves.add(m);
        combine(u, vid);
        addWorkList(u);
      } else {
        activeMoves.add(m);
      }
    }
  }

  private void combine(Node u, Node.Id v) {
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
      if (t instanceof Node.Id tid) {
        decrementDegree(tid);
      }
    }
    if (degree.get(u) >= NUM_REG && u instanceof Node.Id uid && freezeWorklist.contains(uid)) {
      freezeWorklist.remove(uid);
      spillWorklist.add(uid);
    }
  }

  private Node getAlias(Node n) {
    Node current = n;
    while (coalescedNodes.contains(current)) {
      current = alias.get(current);
    }
    return current;
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
      if (v instanceof Node.Id vid && freezeWorklist.contains(vid) && nodeMoves(v).isEmpty()) {
        freezeWorklist.remove(vid);
        simplifyWorklist.add(vid);
      }
    }
  }

  private Map<Node.Id, Integer> calculateNodeDefUse(Method method) {
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
    return Collections.unmodifiableMap(counter);
  }

  private Map<Node.Id, Double> calculateSpillPriority(
      Map<Node.Id, Integer> nodeDefUse, Set<String> stackArgIdList) {
    final var preliminaryScores =
        nodeDefUse.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    e -> (double) e.getValue() / (double) degree.get(e.getKey())));
    // Prioritise already spilled stack arguments
    final var stackArgZeroedScores =
        preliminaryScores.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    e -> stackArgIdList.contains(e.getKey().id()) ? 0 : e.getValue()));
    // Add the max score to temporary generated nodes to make them less prioritised (those have
    // short lifetimes)
    // Also, make already spilled temporaries score even higher so they are even less likely to be
    // chosen.
    final var maxScore =
        stackArgZeroedScores.values().stream()
            .filter(Double::isFinite)
            .mapToDouble(Double::doubleValue)
            .max()
            .getAsDouble();
    final var result =
        stackArgZeroedScores.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    e ->
                        e.getValue()
                            + (e.getKey().id().startsWith("_@") ? maxScore : 0)
                            + (e.getKey().id().startsWith("_") ? maxScore : 0)));
    return result;
  }

  private void selectSpill(Map<Node.Id, Integer> nodeDefUse, Set<String> stackArgIdList) {
    final var spillPriorityScore = calculateSpillPriority(nodeDefUse, stackArgIdList);
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

  private Method rewriteProgram(Method method) {
    var stmtList = method.lowerStmtList();
    var vars = method.vars();
    for (final var v : spilledNodes) {
      final var id = v.id();
      final var type =
          Stream.concat(method.vars().stream(), method.argsWithThis().stream())
              .filter(va -> va.id().equals(id))
              .findFirst()
              .get()
              .type();
      final var newStmtList = new ArrayList<LowerStmt>();
      final var gen = new TempVarGen("_@" + id);
      for (final var stmt : method.lowerStmtList()) {
        final List<LowerStmt> stmtChunk =
            switch (stmt.stmtExtensionType()) {
              case BIT -> {
                final var bs = (BitLowerStmt) stmt;
                if (!Stream.of(bs.dest().id(), bs.expr().id())
                    .collect(Collectors.toSet())
                    .contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var expr = bs.expr().id().equals(id) ? idRvalExpr : bs.expr();
                final var dest = bs.dest().id().equals(id) ? idRvalExpr : bs.dest();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (bs.expr().id().equals(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new BitLowerStmt(bs.op(), dest, expr, bs.shift()));
                // Need to store
                if (bs.dest().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
              case LOAD_STACK_ARG -> {
                final var lsas = (LoadStackArgLowerStmt) stmt;
                if (!lsas.stackArg().id().equals(id)) {
                  yield List.of(stmt);
                }
                yield List.of();
              }
              case REVERSE_SUBTRACT -> {
                final var bs = (ReverseSubtractLowerStmt) stmt;
                final var operands = new HashSet<String>();
                final var loadOperands = new HashSet<String>();
                if (bs.lhs() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                  loadOperands.add(a.idRvalExpr().id());
                }
                if (bs.rhs() instanceof IdRvalExpr a) {
                  operands.add(a.id());
                  loadOperands.add(a.id());
                }
                if (bs.dest() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                }
                if (!operands.contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var idRvalExprAddressable = new Addressable.IdRval(idRvalExpr);
                final var lhs =
                    bs.lhs() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.lhs();
                final var rhs =
                    bs.rhs() instanceof IdRvalExpr a && a.id().equals(id) ? idRvalExpr : bs.rhs();
                final var dest =
                    bs.dest() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.dest();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (loadOperands.contains(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new ReverseSubtractLowerStmt(dest, lhs, rhs));
                // Need to store
                if (bs.dest() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
              case BINARY -> {
                final var bs = (BinaryLowerStmt) stmt;
                final var operands = new HashSet<String>();
                final var loadOperands = new HashSet<String>();
                if (bs.lhs() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                  loadOperands.add(a.idRvalExpr().id());
                }
                if (bs.rhs() instanceof IdRvalExpr a) {
                  operands.add(a.id());
                  loadOperands.add(a.id());
                }
                if (bs.dest() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                }
                if (!operands.contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var idRvalExprAddressable = new Addressable.IdRval(idRvalExpr);
                final var lhs =
                    bs.lhs() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.lhs();
                final var rhs =
                    bs.rhs() instanceof IdRvalExpr a && a.id().equals(id) ? idRvalExpr : bs.rhs();
                final var dest =
                    bs.dest() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.dest();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (loadOperands.contains(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new BinaryLowerStmt(bs.op(), dest, lhs, rhs));
                // Need to store
                if (bs.dest() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
              case REG_BINARY -> {
                final var bs = (RegBinaryLowerStmt) stmt;
                final var operands = new HashSet<String>();
                final var loadOperands = new HashSet<String>();
                if (bs.lhs() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                  loadOperands.add(a.idRvalExpr().id());
                }
                if (bs.rhs() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                  loadOperands.add(a.idRvalExpr().id());
                }
                if (bs.dest() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                }
                if (!operands.contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var idRvalExprAddressable = new Addressable.IdRval(idRvalExpr);
                final var lhs =
                    bs.lhs() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.lhs();
                final var rhs =
                    bs.rhs() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.rhs();
                final var dest =
                    bs.dest() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? idRvalExprAddressable
                        : bs.dest();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (loadOperands.contains(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new RegBinaryLowerStmt(bs.op(), dest, lhs, rhs));
                // Need to store
                if (bs.dest() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
              case CMP -> {
                final var cs = (CmpLowerStmt) stmt;
                if (!(cs.lhs() instanceof IdRvalExpr lhsIre && lhsIre.id().equals(id))
                    && !(cs.rhs() instanceof IdRvalExpr rhsIre && rhsIre.id().equals(id))) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var lhs =
                    (cs.lhs() instanceof IdRvalExpr ire && ire.id().equals(id))
                        ? idRvalExpr
                        : cs.lhs();
                final var rhs =
                    (cs.rhs() instanceof IdRvalExpr ire && ire.id().equals(id))
                        ? idRvalExpr
                        : cs.rhs();
                yield List.of(
                    new LoadSpilledLowerStmt(idRvalExpr, id),
                    new CmpLowerStmt(cs.op(), lhs, rhs, cs.dest()));
              }
              case FIELD_ACCESS -> {
                final var fas = (FieldAccessLowerStmt) stmt;
                if (!Stream.of(fas.lhs().id(), fas.rhsId().id())
                    .collect(Collectors.toSet())
                    .contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var lhs = fas.lhs().id().equals(id) ? idRvalExpr : fas.lhs();
                final var rhsId = fas.rhsId().id().equals(id) ? idRvalExpr : fas.rhsId();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (fas.rhsId().id().equals(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new FieldAccessLowerStmt(lhs, rhsId, fas.rhsField()));
                // Need to store
                if (fas.lhs().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
              case FIELD_ASSIGN -> {
                final var fas = (FieldAssignLowerStmt) stmt;
                final var operands = new HashSet<String>();
                operands.add(fas.lhsId().id());
                if (fas.rhs() instanceof Addressable.IdRval a) {
                  operands.add(a.idRvalExpr().id());
                }
                if (!operands.contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var lhsId = fas.lhsId().id().equals(id) ? idRvalExpr : fas.lhsId();
                final var rhs =
                    fas.rhs() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)
                        ? new Addressable.IdRval(idRvalExpr)
                        : fas.rhs();
                yield List.of(
                    new LoadSpilledLowerStmt(idRvalExpr, id),
                    new FieldAssignLowerStmt(lhsId, fas.lhsField(), rhs));
              }
              case IMMEDIATE -> {
                final var is = (ImmediateLowerStmt) stmt;
                if (!(is.dest() instanceof Addressable.IdRval a
                    && a.idRvalExpr().id().equals(id))) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                yield List.of(
                    new ImmediateLowerStmt(new Addressable.IdRval(idRvalExpr), is.value()),
                    new StoreSpilledLowerStmt(idRvalExpr, id));
              }
              case LABEL, GOTO, RETURN, BRANCH_LINK, POP_STACK, PUSH_PAD_STACK -> List.of(stmt);
              case LDR_SPILL -> {
                final var ls = (LoadSpilledLowerStmt) stmt;
                if (ls.dst().id().equals(id))
                  throw new RuntimeException("No solution to register allocation");
                yield List.of(stmt);
              }
              case STR_SPILL -> {
                final var ss = (StoreSpilledLowerStmt) stmt;
                if (ss.src().id().equals(id))
                  throw new RuntimeException("No solution to register allocation");
                yield List.of(stmt);
              }
              case MOV -> {
                final var ms = (MovLowerStmt) stmt;
                final var idOperands = new HashSet<String>();
                if (ms.src() instanceof Addressable.IdRval a) idOperands.add(a.idRvalExpr().id());
                if (ms.dst() instanceof Addressable.IdRval a) idOperands.add(a.idRvalExpr().id());
                if (!idOperands.contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var src =
                    (ms.src() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id))
                        ? new Addressable.IdRval(idRvalExpr)
                        : ms.src();
                final var dst =
                    (ms.dst() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id))
                        ? new Addressable.IdRval(idRvalExpr)
                        : ms.dst();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (ms.src() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new MovLowerStmt(dst, src));
                if (ms.dst() instanceof Addressable.IdRval a && a.idRvalExpr().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
              case PUSH_STACK -> {
                final var pss = (PushStackLowerStmt) stmt;
                if (!pss.idRvalExpr().id().equals(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                yield List.of(
                    new LoadSpilledLowerStmt(idRvalExpr, id), new PushStackLowerStmt(idRvalExpr));
              }
              case UNARY -> {
                final var us = (UnaryLowerStmt) stmt;
                if (!Stream.of(us.dest().id(), us.expr().id())
                    .collect(Collectors.toSet())
                    .contains(id)) {
                  yield List.of(stmt);
                }
                final var tempVar = gen.gen(type);
                final var idRvalExpr = new IdRvalExpr(tempVar.id());
                final var expr = us.expr().id().equals(id) ? idRvalExpr : us.expr();
                final var dest = us.dest().id().equals(id) ? idRvalExpr : us.dest();
                final var result = new ArrayList<LowerStmt>();
                // Need to load
                if (us.expr().id().equals(id)) {
                  result.add(new LoadSpilledLowerStmt(idRvalExpr, id));
                }
                result.add(new UnaryLowerStmt(us.op(), dest, expr));
                // Need to store
                if (us.dest().id().equals(id)) {
                  result.add(new StoreSpilledLowerStmt(idRvalExpr, id));
                }
                yield Collections.unmodifiableList(result);
              }
            };
        newStmtList.addAll(stmtChunk);
      }
      final var newVars =
          Stream.concat(
                  method.vars().stream().filter(va -> !va.id().equals(id)), gen.getVars().stream())
              .collect(Collectors.toUnmodifiableList());
      stmtList = Collections.unmodifiableList(newStmtList);
      vars = Collections.unmodifiableList(newVars);
    }
    final Set<Var> stackArgs =
        method.argsWithThis().size() > 4
            ? method.argsWithThis().subList(4, method.argsWithThis().size()).stream()
                .collect(Collectors.toUnmodifiableSet())
            : Set.of();
    final var newSpilled =
        Stream.concat(method.argsWithThis().stream(), method.vars().stream())
            .filter(v -> spilledNodes.contains(new Node.Id(v.id())))
            .collect(Collectors.toUnmodifiableList());
    final var spilled =
        Stream.concat(method.spilled().stream(), newSpilled.stream())
            .filter(v -> !stackArgs.contains(v))
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(), method.id(), method.argsWithThis(), vars, spilled, stmtList);
  }
}
