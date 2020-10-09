package jlitec.ir3.stmt;

import jlitec.ir3.expr.BinaryExpr;

public record CmpStmt(BinaryExpr condition, LabelStmt dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.CMP;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("if (")
        .append(condition.print(indent))
        .append(") goto ")
        .append(dest.label())
        .append(";\n");
    return sb.toString();
  }
}
