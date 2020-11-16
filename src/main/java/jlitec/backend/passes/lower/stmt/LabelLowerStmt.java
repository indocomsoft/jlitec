package jlitec.backend.passes.lower.stmt;

public record LabelLowerStmt(String label) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.LABEL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, Math.max(indent - 1, 0));
    sb.append(label).append(":\n");
    return sb.toString();
  }
}
