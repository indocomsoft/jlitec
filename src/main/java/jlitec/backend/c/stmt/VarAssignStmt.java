package jlitec.backend.c.stmt;

import jlitec.backend.c.expr.Expr;

public record VarAssignStmt(String lhs, Expr rhs) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.VAR_ASSIGN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhs).append(" = ").append(rhs.print(indent)).append(";\n");
    return sb.toString();
  }
}
