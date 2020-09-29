package jlitec.ast.stmt;

import java.util.Optional;
import jlitec.ast.expr.Expr;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record ReturnStmt(Optional<Expr> maybeExpr, Location leftLocation, Location rightLocation) implements Stmt {
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
