package jlitec.backend.c.stmt;

import jlitec.backend.c.expr.Expr;

public record IfStmt(Expr condition, String dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.IF;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("if (").append(condition.print(indent)).append(") goto ").append(dest).append(";\n");
    return sb.toString();
  }
}
