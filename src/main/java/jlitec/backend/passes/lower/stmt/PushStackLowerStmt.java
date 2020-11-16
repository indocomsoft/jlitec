package jlitec.backend.passes.lower.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record PushStackLowerStmt(List<IdRvalExpr> elements) implements LowerStmt {
  public PushStackLowerStmt {
    this.elements = Collections.unmodifiableList(elements);
  }

  @Override
  public LowerStmtType stmtExtensionType() {
    return LowerStmtType.PUSH_STACK;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("PUSH ");
    sb.append(elements.stream().map(IdRvalExpr::id).collect(Collectors.joining(", ")));
    sb.append(";\n");
    return sb.toString();
  }
}
