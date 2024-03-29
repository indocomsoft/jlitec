package jlitec.parsetree.stmt;

import java_cup.runtime.ComplexSymbolFactory.Location;
import jlitec.parsetree.expr.Expr;

public record FieldAssignStmt(
    Expr lhsTarget, String lhsId, Expr rhs, Location leftLocation, Location rightLocation)
    implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_FIELD_ASSIGN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhsTarget.print(indent))
        .append('.')
        .append(lhsId)
        .append(" = ")
        .append(rhs.print(indent))
        .append(";\n");
    return sb.toString();
  }
}
