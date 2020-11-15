package jlitec.backend.passes.live;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jlitec.backend.passes.Pass;
import jlitec.backend.passes.flow.Block;
import jlitec.backend.passes.flow.FlowGraph;
import jlitec.ir3.expr.BinaryExpr;
import jlitec.ir3.expr.CallExpr;
import jlitec.ir3.expr.Expr;
import jlitec.ir3.expr.FieldExpr;
import jlitec.ir3.expr.UnaryExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.stmt.CallStmt;
import jlitec.ir3.stmt.CmpStmt;
import jlitec.ir3.stmt.FieldAssignStmt;
import jlitec.ir3.stmt.PrintlnStmt;
import jlitec.ir3.stmt.ReadlnStmt;
import jlitec.ir3.stmt.ReturnStmt;
import jlitec.ir3.stmt.Stmt;
import jlitec.ir3.stmt.VarAssignStmt;

public class LivePass implements Pass<MethodWithFlow, Method> {
  private record DefUse(Set<String> use, Set<String> def) {
    public static DefUse EMPTY = new DefUse(Set.of(), Set.of());

    public DefUse {
      this.use = Collections.unmodifiableSet(use);
      this.def = Collections.unmodifiableSet(def);
    }

    public static DefUse combine(DefUse defUse1, DefUse defUse2) {
      return new DefUse(Sets.union(defUse1.use, defUse2.use), Sets.union(defUse1.def, defUse2.def));
    }
  }

  private record InOut(SetMultimap<Integer, String> in, SetMultimap<Integer, String> out) {
    public InOut {
      this.in = Multimaps.unmodifiableSetMultimap(in);
      this.out = Multimaps.unmodifiableSetMultimap(out);
    }
  }

  @Override
  public Method pass(MethodWithFlow input) {
    final var defUseList =
        input.flowGraph().blocks().stream()
            .map(this::calculateDefUse)
            .collect(Collectors.toUnmodifiableList());
    final var inOut = dataflow(defUseList, input.flowGraph());
    final var blockWithDefUseList =
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

    return new Method(
        input.method().returnType(),
        input.method().id(),
        input.method().argsWithThis(),
        input.method().vars(),
        blockWithDefUseList);
  }

  private InOut dataflow(List<DefUse> defUseList, FlowGraph flowGraph) {
    final var in = HashMultimap.<Integer, String>create();
    final var out = HashMultimap.<Integer, String>create();
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < flowGraph.blocks().size(); i++) {
        final var block = flowGraph.blocks().get(i);
        final var defUse = defUseList.get(i);
        final var def = defUse.def();
        final var use = defUse.use();

        // OUT[B] = U_{S a successor of B} IN[S];
        final var newOut = new HashSet<String>();
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

  private DefUse calculateDefUse(Block block) {
    return switch (block.type()) {
      case EXIT -> DefUse.EMPTY;
      case BASIC -> {
        final var bb = (Block.Basic) block;
        final var use = new HashSet<String>();
        final var def = new HashSet<String>();
        for (final var stmt : Lists.reverse(bb.stmtList())) {
          final var defUse = calculateDefUse(stmt);
          use.removeAll(defUse.def());
          use.addAll(defUse.use());
          def.addAll(defUse.def());
        }
        yield new DefUse(use, def);
      }
    };
  }

  private DefUse calculateDefUse(Stmt stmt) {
    return switch (stmt.getStmtType()) {
      case LABEL, GOTO -> DefUse.EMPTY;
      case CMP -> {
        final var cs = (CmpStmt) stmt;
        yield calculateDefUse(cs.condition());
      }
      case READLN -> {
        final var rs = (ReadlnStmt) stmt;
        yield new DefUse(Set.of(), Set.of(rs.dest().id()));
      }
      case PRINTLN -> {
        final var ps = (PrintlnStmt) stmt;
        yield calculateDefUse(ps.rval());
      }
      case VAR_ASSIGN -> {
        final var vas = (VarAssignStmt) stmt;
        yield DefUse.combine(
            new DefUse(Set.of(), Set.of(vas.lhs().id())), calculateDefUse(vas.rhs()));
      }
      case FIELD_ASSIGN -> {
        final var fas = (FieldAssignStmt) stmt;
        yield DefUse.combine(
            new DefUse(Set.of(fas.lhsId().id()), Set.of()), calculateDefUse(fas.rhs()));
      }
      case CALL -> {
        final var cs = (CallStmt) stmt;
        yield cs.args().stream().map(this::calculateDefUse).reduce(DefUse.EMPTY, DefUse::combine);
      }
      case RETURN -> {
        final var rs = (ReturnStmt) stmt;
        yield rs.maybeValue().map(this::calculateDefUse).orElse(DefUse.EMPTY);
      }
    };
  }

  private DefUse calculateDefUse(Expr expr) {
    return switch (expr.getExprType()) {
      case BINARY -> {
        final var be = (BinaryExpr) expr;
        yield Stream.of(be.lhs(), be.rhs())
            .map(this::calculateDefUse)
            .reduce(DefUse.EMPTY, DefUse::combine);
      }
      case UNARY -> {
        final var ue = (UnaryExpr) expr;
        yield calculateDefUse(ue.rval());
      }
      case FIELD -> {
        final var fe = (FieldExpr) expr;
        yield new DefUse(Set.of(fe.target().id()), Set.of());
      }
      case RVAL -> {
        final var re = (RvalExpr) expr;
        yield switch (re.getRvalExprType()) {
          case ID -> {
            final var ire = (IdRvalExpr) re;
            yield new DefUse(Set.of(ire.id()), Set.of());
          }
          case STRING, INT, BOOL, NULL -> DefUse.EMPTY;
        };
      }
      case CALL -> {
        final var ce = (CallExpr) expr;
        yield ce.args().stream().map(this::calculateDefUse).reduce(DefUse.EMPTY, DefUse::combine);
      }
      case NEW -> DefUse.EMPTY;
    };
  }
}
