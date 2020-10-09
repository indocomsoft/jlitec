package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record CallStmt(Expr target, List<Expr> args, TypeAnnotation typeAnnotation) implements Stmt {
  public CallStmt {
    this.args = Collections.unmodifiableList(args);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_CALL;
  }
}
