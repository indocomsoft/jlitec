package jlitec.ast;

import java.util.Collections;
import java.util.List;
import jlitec.ast.stmt.Stmt;

public record Method(Type type, String id, List<Var> args, List<Var> vars, List<Stmt> stmtList) {
  public Method {
    this.args = Collections.unmodifiableList(args);
    this.vars = Collections.unmodifiableList(vars);
    this.stmtList = Collections.unmodifiableList(stmtList);
  }
}
