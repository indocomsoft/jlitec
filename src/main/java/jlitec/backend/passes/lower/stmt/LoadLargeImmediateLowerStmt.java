package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IntRvalExpr;

public record LoadLargeImmediateLowerStmt(Addressable dest, IntRvalExpr value)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.LOAD_LARGE_IMM;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0)).append(" = ").append(value.print(0)).append("; // LARGE\n");
    return sb.toString();
  }
}
