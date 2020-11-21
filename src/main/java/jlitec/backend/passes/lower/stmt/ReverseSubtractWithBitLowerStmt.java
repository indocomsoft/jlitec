package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record ReverseSubtractWithBitLowerStmt(
    Addressable dest, Addressable lhs, IdRvalExpr rhs, BitOp bitOp, int shift)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.REVERSE_SUBTRACT_BIT;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0))
        .append(" = ")
        .append(" (")
        .append(bitOp.name())
        .append(" ")
        .append(rhs.print(0))
        .append(", #")
        .append(shift)
        .append(") - ")
        .append(lhs.print(0))
        .append(";\n");
    return sb.toString();
  }
}
