package jlitec.ir3.stmt;

import jlitec.ir3.expr.rval.RvalExpr;

public record PrintlnStmt(RvalExpr rval) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.PRINTLN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("println(").append(rval.print(indent)).append(");\n");
    return sb.toString();
  }
}
