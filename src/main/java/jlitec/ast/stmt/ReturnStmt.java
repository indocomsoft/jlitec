package jlitec.ast.stmt;

import java.util.Optional;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record ReturnStmt(Optional<Expr> maybeExpr) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_RETURN;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return maybeExpr.isPresent()
        ? maybeExpr.get().typeAnnotation()
        : new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID);
  }
}
