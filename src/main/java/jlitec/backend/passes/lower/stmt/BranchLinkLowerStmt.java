package jlitec.backend.passes.lower.stmt;

public record BranchLinkLowerStmt(String target) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.BRANCH_LINK;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("CALL ").append(target).append(";\n");
    return sb.toString();
  }
}
