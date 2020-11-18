package jlitec.backend.passes.lower.stmt;

import jlitec.ir3.Var;

public record LoadStackArgLowerStmt(Var stackArg) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.LOAD_STACK_ARG;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("LOAD_STACK_ARG ").append(stackArg.type().print(0)).append(" ").append(stackArg.id());
    sb.append(";\n");
    return sb.toString();
  }
}
