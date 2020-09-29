package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record VarAssignStmt(String lhsId, Expr rhs, Location leftLocation, Location rightLocation) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_VAR_ASSIGN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(lhsId).append(" = ").append(rhs.print(indent)).append(";\n");
    return sb.toString();
  }
}
