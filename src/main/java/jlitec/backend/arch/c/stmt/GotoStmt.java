package jlitec.backend.arch.c.stmt;

public record GotoStmt(String dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.GOTO;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("goto ").append(dest).append(";\n");
    return sb.toString();
  }
}
