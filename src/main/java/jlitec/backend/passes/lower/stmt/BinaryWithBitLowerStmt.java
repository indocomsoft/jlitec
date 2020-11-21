package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record BinaryWithBitLowerStmt(
    BinaryOp op, Addressable dest, Addressable lhs, IdRvalExpr rhs, BitOp bitOp, int shift)
    implements LowerStmt {
  public BinaryWithBitLowerStmt {
    final boolean valid =
        switch (op) {
          case MULT, DIV -> false;
          case PLUS, MINUS, EQ, NEQ, GT, LT, GEQ, LEQ, OR, AND -> true;
        };
    if (!valid) {
      throw new RuntimeException();
    }
  }

  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.BINARY_BIT;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dest.print(0))
        .append(" = ")
        .append(lhs.print(0))
        .append(" ")
        .append(op.print(0))
        .append(" (")
        .append(bitOp.name())
        .append(" ")
        .append(rhs.print(0))
        .append(", #")
        .append(shift)
        .append(");\n");
    return sb.toString();
  }
}
