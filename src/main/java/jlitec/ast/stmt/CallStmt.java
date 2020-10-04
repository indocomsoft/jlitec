package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ast.expr.Expr;

public record CallStmt(Expr target, List<Expr> args) implements Stmt {
  public CallStmt {
    this.args = Collections.unmodifiableList(args);
  }

  public CallStmt(jlitec.parsetree.stmt.CallStmt cs) {
    this(
        Expr.fromParseTree(cs.target()),
        cs.args().stream().map(Expr::fromParseTree).collect(Collectors.toUnmodifiableList()));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_CALL;
  }
}
