package jlitec.ast.stmt;

import java_cup.runtime.ComplexSymbolFactory.Location;

public record ReadlnStmt(String id, Location leftLocation, Location rightLocation) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_READLN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("readln(").append(id).append(");\n");
    return sb.toString();
  }
}
