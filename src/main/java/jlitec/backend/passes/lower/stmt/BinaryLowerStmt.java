package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.BinaryOp;

public record BinaryLowerStmt(BinaryOp op, Addressable dest, Addressable lhs, Addressable rhs)
    implements LowerStmt {
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
