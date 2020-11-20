package jlitec.backend.passes.optimization.deadcode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.Program;
import jlitec.backend.passes.lower.stmt.CmpLowerStmt;
import jlitec.backend.passes.lower.stmt.GotoLowerStmt;
import jlitec.backend.passes.lower.stmt.LabelLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.optimization.OptimizationPass;

public class DeadcodeOptimizationPass implements OptimizationPass {
  @Override
  public Program pass(Program input) {
    Program program = input;
    while (true) {
      final var methodList =
          program.methodList().stream()
              .map(this::passBasicBlock)
              .map(this::passUses)
              .map(this::passDeadGoto)
              .map(this::passRemoveMovItself)
              .map(this::passRemoveUselessLabels)
              .map(this::passRemoveConsecutiveLabels)
              .map(this::passRemoveMovItself)
              .collect(Collectors.toUnmodifiableList());
      final var newProgram = new Program(program.dataList(), methodList);
      if (newProgram.equals(program)) {
        return program;
      }
      program = newProgram;
    }
  }

  private Method passRemoveMovItself(Method method) {
    final var stmtList =
        method.lowerStmtList().stream()
            .filter(stmt -> !(stmt instanceof MovLowerStmt m) || !m.src().equals(m.dst()))
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  private Method passDeadGoto(Method method) {
    if (method.lowerStmtList().size() == 1) {
      return method;
    }

    final Set<Integer> deletedIndices = new HashSet<>();
    for (int i = 0; i < method.lowerStmtList().size() - 1; i++) {
      final var currentStmt = method.lowerStmtList().get(i);
      final var nextStmt = method.lowerStmtList().get(i + 1);
      if (currentStmt instanceof GotoLowerStmt g
          && nextStmt instanceof LabelLowerStmt l
          && g.dest().equals(l.label())) {
        // it is always safe to delete the goto
        deletedIndices.add(i);
      }
    }
    final var stmtList =
        IntStream.range(0, method.lowerStmtList().size())
            .filter(i -> !deletedIndices.contains(i))
            .boxed()
            .map(i -> method.lowerStmtList().get(i))
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  private Method passRemoveConsecutiveLabels(Method method) {
    if (method.lowerStmtList().size() == 1) {
      return method;
    }
    final Map<String, String> coalesced = new HashMap<>();

    final Set<Integer> deletedIndices = new HashSet<>();
    for (int i = 0; i < method.lowerStmtList().size() - 1; i++) {
      final var currentStmt = method.lowerStmtList().get(i);
      final var nextStmt = method.lowerStmtList().get(i + 1);
      if (currentStmt instanceof LabelLowerStmt l1 && nextStmt instanceof LabelLowerStmt l2) {
        coalesced.put(
            l2.label(), coalesced.containsKey(l1.label()) ? coalesced.get(l1.label()) : l1.label());
        deletedIndices.add(i + 1);
      }
    }
    final var stmtList =
        IntStream.range(0, method.lowerStmtList().size())
            .filter(i -> !deletedIndices.contains(i))
            .boxed()
            .map(i -> method.lowerStmtList().get(i))
            .map(
                stmt -> {
                  if (stmt instanceof GotoLowerStmt g && coalesced.containsKey(g.dest())) {
                    return new GotoLowerStmt(coalesced.get(g.dest()));
                  } else if (stmt instanceof CmpLowerStmt c && coalesced.containsKey(c.dest())) {
                    return new CmpLowerStmt(c.op(), c.lhs(), c.rhs(), coalesced.get(c.dest()));
                  }
                  return stmt;
                })
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  private Method passBasicBlock(Method method) {
    final var flow = new FlowPass().pass(method.lowerStmtList());
    final var activeBasicBlocks = eliminateDeadBasicBlock(flow.edges(), flow.blocks().size());
    final var stmtList =
        activeBasicBlocks.stream()
            .sorted()
            .map(i -> flow.blocks().get(i))
            .flatMap(
                b -> b instanceof Block.Basic bb ? bb.lowerStmtList().stream() : Stream.empty())
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  private Method passRemoveUselessLabels(Method method) {
    final var usedLabels =
        method.lowerStmtList().stream()
            .flatMap(
                s -> {
                  if (s instanceof CmpLowerStmt cmp) {
                    return Stream.of(cmp.dest());
                  } else if (s instanceof GotoLowerStmt g) {
                    return Stream.of(g.dest());
                  }
                  return Stream.empty();
                })
            .collect(Collectors.toUnmodifiableSet());
    final var stmtList =
        method.lowerStmtList().stream()
            .filter(s -> !(s instanceof LabelLowerStmt l) || usedLabels.contains(l.label()))
            .collect(Collectors.toUnmodifiableList());
    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  private Method passUses(Method method) {
    final var flow = new FlowPass().pass(method.lowerStmtList());
    final var methodWithLive = new LivePass().pass(new MethodWithFlow(method, flow));
    final var stmtList = new ArrayList<LowerStmt>();
    for (final var stmtWithLive : methodWithLive.lowerStmtWithLiveList()) {
      final var stmt = stmtWithLive.lowerStmt();

      final List<LowerStmt> stmtChunk =
          switch (stmt.stmtExtensionType()) {
            case PUSH_PAD_STACK, BRANCH_LINK, CMP, GOTO, LABEL, RETURN, POP_STACK, PUSH_STACK, LDR_SPILL, STR_SPILL -> List
                .of(stmt);
            case BINARY, FIELD_ACCESS, FIELD_ASSIGN, IMMEDIATE, LOAD_STACK_ARG, MOV, REG_BINARY, UNARY, BIT, REVERSE_SUBTRACT -> {
              final var def = LivePass.calculateDefUse(stmt).def();
              if (def.isEmpty()) {
                // Just to guard
                yield List.of(stmt);
              }
              if (!Sets.difference(stmtWithLive.liveOut(), def).equals(stmtWithLive.liveOut())) {
                yield List.of(stmt);
              }
              // Skip, as this statement does not def anything that will subsequently be used
              yield List.of();
            }
          };
      stmtList.addAll(stmtChunk);
    }
    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  private Set<Integer> eliminateDeadBasicBlock(
      SetMultimap<Integer, Integer> originalEdges, int numBasicBlocks) {
    var activeBasicBlocks =
        ImmutableSet.<Integer>builder()
            .addAll(originalEdges.values())
            .add(0)
            .add(numBasicBlocks - 1)
            .build();
    var edges = HashMultimap.<Integer, Integer>create();
    while (true) {
      for (final var activeBasicBlock : activeBasicBlocks) {
        edges.putAll(activeBasicBlock, originalEdges.get(activeBasicBlock));
      }
      final var newActiveBasicBlocks =
          ImmutableSet.<Integer>builder()
              .addAll(edges.values())
              .add(0)
              .add(numBasicBlocks - 1)
              .build();
      if (activeBasicBlocks.equals(newActiveBasicBlocks)) {
        return activeBasicBlocks;
      }
      activeBasicBlocks = newActiveBasicBlocks;
      edges = HashMultimap.create();
    }
  }
}
