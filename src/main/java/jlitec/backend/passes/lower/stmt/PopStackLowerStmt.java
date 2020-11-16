package jlitec.backend.passes.lower.stmt;

public record PopStackLowerStmt(int num) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.POP_STACK;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("POP ").append(num).append(";\n");
    return sb.toString();
  }
}
