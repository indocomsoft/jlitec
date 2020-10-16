package jlitec.backend.c.stmt;

import jlitec.backend.c.expr.Expr;

public record FieldAssignStmt(String lhsTarget, String lhsField, Expr rhs) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.FIELD_ASSIGN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhsTarget)
        .append("->")
        .append(lhsField)
        .append(" = ")
        .append(rhs.print(indent))
        .append(";\n");
    return sb.toString();
  }
}
