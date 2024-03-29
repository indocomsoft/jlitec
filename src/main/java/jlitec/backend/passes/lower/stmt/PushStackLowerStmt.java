package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record PushStackLowerStmt(IdRvalExpr idRvalExpr) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.PUSH_STACK;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("PUSH ");
    sb.append(idRvalExpr.print(0));
    sb.append(";\n");
    return sb.toString();
  }
}
