package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ast.expr.Expr;

public record WhileStmt(Expr condition, List<Stmt> stmtList) implements Stmt {
  public WhileStmt {
    this.stmtList = Collections.unmodifiableList(stmtList);
  }

  public WhileStmt(jlitec.parsetree.stmt.WhileStmt ws) {
    this(
        Expr.fromParseTree(ws.condition()),
        ws.stmtList().stream().map(Stmt::fromParseTree).collect(Collectors.toUnmodifiableList()));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_WHILE;
  }
}
