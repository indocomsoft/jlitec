package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ast.expr.Expr;

public record IfStmt(Expr condition, List<Stmt> thenStmtList, List<Stmt> elseStmtList)
    implements Stmt {
  public IfStmt {
    this.thenStmtList = Collections.unmodifiableList(thenStmtList);
    this.elseStmtList = Collections.unmodifiableList(elseStmtList);
  }

  public IfStmt(jlitec.parsetree.stmt.IfStmt is) {
    this(
        Expr.fromParseTree(is.condition()),
        is.thenStmtList().stream()
            .map(Stmt::fromParseTree)
            .collect(Collectors.toUnmodifiableList()),
        is.elseStmtList().stream()
            .map(Stmt::fromParseTree)
            .collect(Collectors.toUnmodifiableList()));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_IF;
  }
}
