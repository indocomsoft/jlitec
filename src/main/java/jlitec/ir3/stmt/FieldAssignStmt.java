package jlitec.ir3.stmt;

import jlitec.ir3.expr.Expr;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record FieldAssignStmt(IdRvalExpr lhsId, String lhsField, Expr rhs) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.FIELD_ASSIGN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhsId.print(indent))
        .append(".")
        .append(lhsField)
        .append(" = ")
        .append(rhs.print(indent))
        .append(";\n");
    return sb.toString();
  }
}
