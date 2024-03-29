package jlitec.ir3.stmt;

import java.util.Optional;
import jlitec.ir3.expr.rval.IdRvalExpr;

public record ReturnStmt(Optional<IdRvalExpr> maybeValue) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.RETURN;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("return");
    maybeValue.ifPresent(idRvalExpr -> sb.append(" ").append(idRvalExpr.print(indent)));
    sb.append(";\n");
    return sb.toString();
  }
}
