package jlitec.parsetree.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java_cup.runtime.ComplexSymbolFactory.Location;
import jlitec.parsetree.expr.Expr;

public record CallStmt(Expr target, List<Expr> args, Location leftLocation, Location rightLocation)
    implements Stmt {
  public CallStmt {
    this.args = Collections.unmodifiableList(args);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_CALL;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(target.print(indent)).append('(');
    sb.append(args.stream().map(arg -> arg.print(indent)).collect(Collectors.joining(", ")));
    sb.append(");\n");
    return sb.toString();
  }
}
