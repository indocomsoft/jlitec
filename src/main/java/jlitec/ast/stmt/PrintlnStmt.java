package jlitec.ast.stmt;

import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record PrintlnStmt(Expr expr) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_PRINTLN;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID);
  }
}
