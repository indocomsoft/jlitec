package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record FieldAccessLowerStmt(IdRvalExpr lhs, IdRvalExpr rhsId, String rhsField)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.FIELD_ACCESS;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhs.print(0))
        .append(" = ")
        .append(rhsId.print(0))
        .append(".")
        .append(rhsField)
        .append(";\n");
    return sb.toString();
  }
}
