package jlitec.ast.stmt;

public record ReadlnStmt(String id) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_READLN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("readln(").append(id).append(");\n");
    return sb.toString();
  }
}
