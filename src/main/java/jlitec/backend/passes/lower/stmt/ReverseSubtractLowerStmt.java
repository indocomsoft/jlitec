package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.RvalExpr;

public record ReverseSubtractLowerStmt(Addressable dest, Addressable lhs, RvalExpr rhs)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.REVERSE_SUBTRACT;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0))
        .append(" = ")
        .append(rhs.print(0))
        .append(" - ")
        .append(lhs.print(0))
        .append(";\n");
    return sb.toString();
  }
}
