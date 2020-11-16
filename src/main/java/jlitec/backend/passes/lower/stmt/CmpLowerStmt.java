package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.rval.RvalExpr;

public record CmpLowerStmt(BinaryOp op, RvalExpr lhs, RvalExpr rhs, String dest)
    implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.CMP;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("if (")
        .append(lhs.print(0))
        .append(" ")
        .append(op.print(0))
        .append(" ")
        .append(rhs.print(0))
        .append(") goto ")
        .append(dest)
        .append(";\n");
    return sb.toString();
  }
}
