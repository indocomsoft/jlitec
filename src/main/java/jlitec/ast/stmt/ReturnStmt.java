package jlitec.ast.stmt;

import java.util.Optional;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record ReturnStmt(Optional<Expr> maybeExpr, TypeAnnotation typeAnnotation) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_RETURN;
  }
}
