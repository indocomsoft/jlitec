package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import jlitec.ast.expr.Expr;

public record WhileStmt(Expr condition, List<Stmt> stmtList) implements Stmt {
  public WhileStmt {
    this.stmtList = Collections.unmodifiableList(stmtList);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_WHILE;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("while (").append(condition.print(indent)).append(") {\n");

    for (final var stmt : stmtList) {
      sb.append(stmt.print(indent + 1));
    }

    indent(sb, indent);
    sb.append("}\n");
    return sb.toString();
  }
}
