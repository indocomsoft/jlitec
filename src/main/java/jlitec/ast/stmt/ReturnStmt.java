package jlitec.ast.stmt;

import java.util.Optional;
import jlitec.ast.expr.Expr;

public record ReturnStmt(Optional<Expr> maybeExpr) implements Stmt {
  public ReturnStmt(jlitec.parsetree.stmt.ReturnStmt rs) {
    this(rs.maybeExpr().map(Expr::fromParseTree));
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_RETURN;
  }
}
