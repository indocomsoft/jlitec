package jlitec.ast.stmt;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record WhileStmt(Expr condition, List<Stmt> stmtList) implements Stmt {
  public WhileStmt {
    this.stmtList = Collections.unmodifiableList(stmtList);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_WHILE;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return stmtList.isEmpty() ? new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID) : Iterables.getLast(stmtList).typeAnnotation();
  }
}
