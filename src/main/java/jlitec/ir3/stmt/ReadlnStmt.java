package jlitec.ir3.stmt;

import jlitec.ir3.expr.rval.IdRvalExpr;

public record ReadlnStmt(IdRvalExpr dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.READLN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("readln(").append(dest.print(indent)).append(");\n");
    return sb.toString();
  }
}
