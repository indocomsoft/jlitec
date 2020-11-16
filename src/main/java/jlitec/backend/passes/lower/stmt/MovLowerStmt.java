package jlitec.backend.passes.lower.stmt;

public record MovLowerStmt(Addressable dst, Addressable src) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.MOV;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(dst.print(0)).append(" <- ").append(src.print(0)).append(";\n");
    return sb.toString();
  }
}
