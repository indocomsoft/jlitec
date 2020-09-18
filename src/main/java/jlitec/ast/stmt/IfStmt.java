package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import jlitec.ast.expr.Expr;

public record IfStmt(Expr condition, List<Stmt> thenStmtList, List<Stmt> elseStmtList)
    implements Stmt {
  public IfStmt {
    this.thenStmtList = Collections.unmodifiableList(thenStmtList);
    this.elseStmtList = Collections.unmodifiableList(elseStmtList);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_IF;
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();

    indent(sb, indent);
    sb.append("if (");
    sb.append(condition.print(indent));
    sb.append(") {\n");

    thenStmtList.forEach(stmt -> sb.append(stmt.print(indent + 1)));

    indent(sb, indent);
    sb.append("} else {\n");

    elseStmtList.forEach(stmt -> sb.append(stmt.print(indent + 1)));

    indent(sb, indent);
    sb.append("}\n");

    return sb.toString();
  }
}
