package jlitec.ast.stmt;

import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.Expr;

public record IfStmt(Expr condition, List<Stmt> thenStmtList, List<Stmt> elseStmtList)
    implements Stmt {
  public IfStmt {
    this.thenStmtList = Collections.unmodifiableList(thenStmtList);
    this.elseStmtList = Collections.unmodifiableList(elseStmtList);
  }

  @Override
  public StmtType getStmtType() {
    return StmtType.STMT_IF;
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return Iterables.getLast(elseStmtList).typeAnnotation();
  }
}
