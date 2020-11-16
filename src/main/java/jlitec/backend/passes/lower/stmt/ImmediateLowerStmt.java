package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.RvalExpr;
import jlitec.ir3.expr.rval.RvalExprType;

public record ImmediateLowerStmt(Addressable dest, RvalExpr value) implements LowerStmt {
  public ImmediateLowerStmt {
    if (value.getRvalExprType() == RvalExprType.ID) {
      throw new RuntimeException("value must be immediate value");
    }
  }

  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.IMMEDIATE;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0)).append(" = ").append(value.print(0)).append(";\n");
    return sb.toString();
  }
}
