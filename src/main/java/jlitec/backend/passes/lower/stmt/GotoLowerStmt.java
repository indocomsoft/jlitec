package jlitec.backend.passes.lower.stmt;

public record GotoLowerStmt(String dest) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.GOTO;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("goto ").append(dest).append(";\n");
    return sb.toString();
  }
}
