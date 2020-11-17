package jlitec.backend.passes.lower.stmt;

import java.util.List;
import java.util.stream.Collectors;

public record LoadStackArgLowerStmt(List<String> stackArgs) implements LowerStmt {
  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.LOAD_STACK_ARG;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("LOAD_STACK_ARG ");
    sb.append(stackArgs.stream().collect(Collectors.joining(", ")));
    sb.append(";\n");
    return sb.toString();
  }
}
