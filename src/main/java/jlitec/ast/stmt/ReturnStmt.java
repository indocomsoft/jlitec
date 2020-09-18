package jlitec.ast.stmt;

import java.util.Optional;
import jlitec.ast.expr.Expr;

public record ReturnStmt(Optional<Expr> maybeExpr) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_RETURN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("return");
    maybeExpr.ifPresent(expr -> sb.append(" ").append(expr.print(indent)));
    sb.append(";\n");
    return sb.toString();
  }
}
