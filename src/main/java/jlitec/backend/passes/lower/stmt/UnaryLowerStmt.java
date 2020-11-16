package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.UnaryOp;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record UnaryLowerStmt(UnaryOp op, IdRvalExpr dest, IdRvalExpr expr) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.UNARY;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0)).append(" = ").append(op.print(0)).append(expr.print(0)).append(";\n");
    return sb.toString();
  }
}
