package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record FieldAssignLowerStmt(IdRvalExpr lhsId, String lhsField, Addressable rhs)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.FIELD_ASSIGN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhsId.print(0))
        .append(".")
        .append(lhsField)
        .append(" = ")
        .append(rhs.print(0))
        .append(";\n");
    return sb.toString();
  }
}
