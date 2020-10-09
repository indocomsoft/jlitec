package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import jlitec.ast.MethodReference;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record CallStmt(
    Expr target, List<Expr> args, TypeAnnotation typeAnnotation, MethodReference methodReference)
    implements Stmt {
  public CallStmt {
    this.args = Collections.unmodifiableList(args);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_CALL;
  }
}
