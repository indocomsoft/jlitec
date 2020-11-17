package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record LoadSpilledLowerStmt(IdRvalExpr dst, String varName) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.LDR_SPILL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("LDR ").append(dst.print(0)).append(" <- ").append(varName).append(";\n");
    return sb.toString();
  }
}
