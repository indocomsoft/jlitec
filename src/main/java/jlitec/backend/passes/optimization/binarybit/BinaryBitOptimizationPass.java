package jlitec.backend.passes.optimization.binarybit;

import java.util.ArrayList;
import java.util.stream.Collectors;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.Program;
import jlitec.backend.passes.lower.stmt.Addressable;
import jlitec.backend.passes.lower.stmt.BinaryLowerStmt;
import jlitec.backend.passes.lower.stmt.BinaryWithBitLowerStmt;
import jlitec.backend.passes.lower.stmt.BitLowerStmt;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.backend.passes.lower.stmt.ReverseSubtractWithBitLowerStmt;
import jlitec.backend.passes.optimization.OptimizationPass;
import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.rval.IdRvalExpr;

public class BinaryBitOptimizationPass implements OptimizationPass {
  @Override
  public Program pass(Program input) {
    Program program = input;
    while (true) {
      final var methodList =
          input.methodList().stream().map(this::pass).collect(Collectors.toUnmodifiableList());
      final var newProgram = new Program(input.dataList(), methodList);
      if (newProgram.equals(program)) {
        return program;
      }
      program = newProgram;
    }
  }

  private Method pass(Method method) {
    final var stmtList = new ArrayList<LowerStmt>();

    for (int i = 0; i < method.lowerStmtList().size(); i++) {
      final var stmt = method.lowerStmtList().get(i);
      if (i == method.lowerStmtList().size() - 1) {
        stmtList.add(stmt);
        continue;
      }
      if (!(stmt instanceof BitLowerStmt bitLowerStmt)) {
        stmtList.add(stmt);
        continue;
      }
      final var nextStmt = method.lowerStmtList().get(i + 1);
      if (nextStmt instanceof BinaryLowerStmt binaryLowerStmt) {
        if (binaryLowerStmt.lhs() instanceof Addressable.IdRval lhs
            && lhs.idRvalExpr().id().equals(bitLowerStmt.dest().id())
            && binaryLowerStmt.rhs() instanceof IdRvalExpr rhs) {
          // Pattern matched
          stmtList.add(stmt);
          if (binaryLowerStmt.op() == BinaryOp.MINUS) {
            stmtList.add(
                new ReverseSubtractWithBitLowerStmt(
                    binaryLowerStmt.dest(),
                    new Addressable.IdRval(rhs),
                    bitLowerStmt.expr(),
                    bitLowerStmt.op(),
                    bitLowerStmt.shift()));
          } else {
            stmtList.add(
                new BinaryWithBitLowerStmt(
                    binaryLowerStmt.op(),
                    binaryLowerStmt.dest(),
                    new Addressable.IdRval(rhs),
                    bitLowerStmt.expr(),
                    bitLowerStmt.op(),
                    bitLowerStmt.shift()));
          }
          i++;
          continue;
        }
        if (binaryLowerStmt.rhs() instanceof IdRvalExpr rhs
            && rhs.id().equals(bitLowerStmt.dest().id())) {
          // Pattern matched
          stmtList.add(stmt);
          stmtList.add(
              new BinaryWithBitLowerStmt(
                  binaryLowerStmt.op(),
                  binaryLowerStmt.dest(),
                  binaryLowerStmt.lhs(),
                  bitLowerStmt.expr(),
                  bitLowerStmt.op(),
                  bitLowerStmt.shift()));
          i++;
          continue;
        }
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
