package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;

public record ReturnStmt(Expr expr) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_RETURN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("return");
    if (expr != null) {
      sb.append(" ").append(expr.print(indent));
    }
    sb.append(";\n");
    return sb.toString();
  }
}
