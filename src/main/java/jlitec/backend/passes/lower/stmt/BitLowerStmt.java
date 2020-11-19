package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record BitLowerStmt(BitOp op, IdRvalExpr dest, IdRvalExpr expr, int shift)
    implements LowerStmt {
  public BitLowerStmt {
    final boolean valid =
        switch (op) {
            //      case ASR, LSR -> shift >= 1 && shift <= 32;
          case LSL -> shift >= 0 && shift <= 31;
            //      case ROR -> shift >= 1 && shift <= 31;
        };
    if (!valid) {
      throw new RuntimeException("Invalid number of shifts");
    }
  }

  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.BIT;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0))
        .append(" <- ")
        .append(op.name())
        .append(" ")
        .append(expr.print(0))
        .append(", #")
        .append(shift)
        .append(";\n");
    return sb.toString();
  }
}
