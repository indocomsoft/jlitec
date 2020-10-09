package jlitec.ir3.stmt;

public record GotoStmt(LabelStmt dest) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.GOTO;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("goto ").append(dest.label()).append(";\n");
    return sb.toString();
  }
}
