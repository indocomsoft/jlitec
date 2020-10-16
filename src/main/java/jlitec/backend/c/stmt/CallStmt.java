package jlitec.backend.c.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.backend.c.expr.Expr;

public record CallStmt(String methodName, List<Expr> args) implements Stmt {
  public CallStmt {
    this.args = Collections.unmodifiableList(args);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.CALL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(methodName).append('(');
    sb.append(args.stream().map(a -> a.print(indent)).collect(Collectors.joining(", ")));
    sb.append(");\n");
    return sb.toString();
  }
}
