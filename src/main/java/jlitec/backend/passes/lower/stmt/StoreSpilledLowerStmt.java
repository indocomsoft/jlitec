package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record StoreSpilledLowerStmt(IdRvalExpr src, String varName) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.STR_SPILL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("STR ").append(src.print(0)).append(" -> ").append(varName).append(";\n");
    return sb.toString();
  }
}
