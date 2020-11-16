package jlitec.backend.passes.lower.stmt;

public record ReturnLowerStmt() implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.RETURN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("return;\n");
    return sb.toString();
  }
}
