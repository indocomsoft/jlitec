package jlitec.ir3.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public record CallStmt(IdRvalExpr target, List<RvalExpr> args) implements Stmt {
  public CallStmt {
    args = Collections.unmodifiableList(args);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.CALL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(target.print(indent)).append('(');
    sb.append(args.stream().map(a -> a.print(indent)).collect(Collectors.joining(", ")));
    sb.append(");\n");
    return sb.toString();
  }
}
