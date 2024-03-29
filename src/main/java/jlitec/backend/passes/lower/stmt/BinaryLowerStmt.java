package jlitec.backend.passes.lower.stmt;

import java.util.EnumSet;
import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.rval.RvalExpr;

public record BinaryLowerStmt(BinaryOp op, Addressable dest, Addressable lhs, RvalExpr rhs)
    implements LowerStmt {
  public BinaryLowerStmt {
    if (EnumSet.of(BinaryOp.MULT, BinaryOp.DIV).contains(op)) {
      throw new RuntimeException("Not a operand2 binary statement");
    }
  }

  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.BINARY;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0))
        .append(" = ")
        .append(lhs.print(0))
        .append(" ")
        .append(op.print(0))
        .append(" ")
        .append(rhs.print(0))
        .append(";\n");
    return sb.toString();
  }
}
