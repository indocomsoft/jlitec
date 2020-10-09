package jlitec.ast.stmt;

import jlitec.ast.TypeAnnotation;

public record ReadlnStmt(String id) implements Stmt {
  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_READLN;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID);
  }
}
