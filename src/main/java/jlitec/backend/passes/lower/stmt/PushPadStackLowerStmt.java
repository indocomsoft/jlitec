package jlitec.backend.passes.lower.stmt;

public record PushPadStackLowerStmt() implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.PUSH_PAD_STACK;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("PUSH (PAD);\n");
    return sb.toString();
  }
}
