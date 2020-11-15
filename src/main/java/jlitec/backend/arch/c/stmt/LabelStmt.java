package jlitec.backend.arch.c.stmt;

public record LabelStmt(String label) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.LABEL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, Math.max(indent - 1, 0));
    sb.append(label).append(":\n");
    return sb.toString();
  }
}
