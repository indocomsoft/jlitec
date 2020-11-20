package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public record BooleanNeqLowerStmt(IdRvalExpr dst, IdRvalExpr lhs, RvalExpr rhs)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.BOOLEAN_NEQ;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dst.print(0))
        .append(" = ")
        .append(lhs.print(0))
        .append(" != ")
        .append(rhs.print(0))
        .append(";\n");
    return sb.toString();
  }
}
