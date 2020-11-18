package jlitec.backend.passes.lower.stmt;

import java.util.EnumSet;
import jlitec.ir3.expr.BinaryOp;

public record RegBinaryLowerStmt(BinaryOp op, Addressable dest, Addressable lhs, Addressable rhs)
    implements LowerStmt {
  public RegBinaryLowerStmt {
    if (!EnumSet.of(BinaryOp.MULT, BinaryOp.DIV).contains(op)) {
      throw new RuntimeException("Not a register-only binary statement");
    }
  }

  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.REG_BINARY;
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
