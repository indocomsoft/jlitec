package jlitec.ast.stmt;

import jlitec.ast.expr.Expr;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record PrintlnStmt(Expr expr, Location leftLocation, Location rightLocation) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_PRINTLN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("println(").append(expr.print(indent)).append(");\n");
    return sb.toString();
  }
}
