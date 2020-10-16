package jlitec.backend.c.stmt;

import java.util.Optional;

public record ReturnStmt(Optional<String> maybeId) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.RETURN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("return");
    maybeId.ifPresent(s -> sb.append(" ").append(s));
    sb.append(";\n");
    return sb.toString();
  }
}
