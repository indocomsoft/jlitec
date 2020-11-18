package jlitec.backend.passes.optimization.algebraic;

import java.util.ArrayList;
import java.util.stream.Collectors;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.Program;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.ImmediateLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.MovLowerStmt;
import jlitec.backend.passes.lower.stmt.RegBinaryLowerStmt;
import jlitec.backend.passes.optimization.OptimizationPass;
import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;

public class AlgebraicPass implements OptimizationPass {
  @Override
  public Program pass(Program input) {
    Program program = input;
    while (true) {
      final var methodList =
          input.methodList().stream()
              .map(this::passPlusMinusZero)
              .map(this::passMultiplyDivideOne)
              .map(this::passAndFalse)
              .map(this::passOrTrue)
              .collect(Collectors.toUnmodifiableList());
      final var newProgram = new Program(input.dataList(), methodList);
      if (newProgram.equals(program)) {
        return program;
      }
      program = newProgram;
    }
  }

  /**
   * Match: <code>
   * t1 = x || true;
   * </code> turn it into: <code>
   *   t1 = true;
   * </code>
   */
  private Method passOrTrue(Method method) {
    final var stmtList = new ArrayList<LowerStmt>();

    for (final var stmt : method.lowerStmtList()) {
      if (stmt instanceof BinaryLowerStmt b
          && b.op() == BinaryOp.OR
          && b.rhs() instanceof BoolRvalExpr bre
          && bre.value()) {
        stmtList.add(new ImmediateLowerStmt(b.dest(), new BoolRvalExpr(true)));
      } else {
        stmtList.add(stmt);
      }
    }

    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  /**
   * Match: <code>
   * t1 = x && false;
   * </code> turn it into: <code>
   *   t1 = false;
   * </code>
   */
  private Method passAndFalse(Method method) {
    final var stmtList = new ArrayList<LowerStmt>();

    for (final var stmt : method.lowerStmtList()) {
      if (stmt instanceof BinaryLowerStmt b
          && b.op() == BinaryOp.AND
          && b.rhs() instanceof BoolRvalExpr bre
          && !bre.value()) {
        stmtList.add(new ImmediateLowerStmt(b.dest(), new BoolRvalExpr(false)));
      } else {
        stmtList.add(stmt);
      }
    }

    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  /**
   * Match: <code>
   * t1 = x + 0;
   * </code> OR <code>
   * t1 = x - 0;
   * </code> turn them into: <code>
   *   t1 = x;
   * </code>
   */
  private Method passPlusMinusZero(Method method) {
    final var stmtList = new ArrayList<LowerStmt>();

    for (final var stmt : method.lowerStmtList()) {
      if (stmt instanceof BinaryLowerStmt b
          && b.rhs() instanceof IntRvalExpr ire
          && ire.value() == 0) {
        stmtList.add(new MovLowerStmt(b.dest(), b.lhs()));
      } else {
        stmtList.add(stmt);
      }
    }

    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }

  /**
   * Match: <code>
   * t0 = 1;
   * t1 = x * t0;
   * </code> OR <code>
   * t0 = 1;
   * t1 = x / t0;
   * </code> turn them into: <code>
   *   t1 = x;
   * </code>
   */
  private Method passMultiplyDivideOne(Method method) {
    final var stmtList = new ArrayList<LowerStmt>();

    for (int i = 0; i < method.lowerStmtList().size(); i++) {
      final var stmt = method.lowerStmtList().get(i);
      if (i == method.lowerStmtList().size() - 1) {
        stmtList.add(stmt);
        continue;
      }
      if (!(stmt instanceof ImmediateLowerStmt imm
          && imm.value() instanceof IntRvalExpr ire
          && ire.value() == 1)) {
        stmtList.add(stmt);
        continue;
      }
      final var nextStmt = method.lowerStmtList().get(i + 1);
      if (nextStmt instanceof RegBinaryLowerStmt r && r.rhs().equals(imm.dest())) {
        // Pattern matched
        stmtList.add(new MovLowerStmt(r.dest(), r.lhs()));
        i++;
        continue;
      }
      if (nextStmt instanceof RegBinaryLowerStmt r && r.lhs().equals(imm.dest())) {
        // Pattern matched
        stmtList.add(new MovLowerStmt(r.dest(), r.rhs()));
        i++;
        continue;
      }
      stmtList.add(stmt);
    }

    return new Method(
        method.returnType(),
        method.id(),
        method.argsWithThis(),
        method.vars(),
        method.spilled(),
        stmtList);
  }
}
