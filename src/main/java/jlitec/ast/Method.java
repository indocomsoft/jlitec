package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ast.stmt.Stmt;

public record Method(
    Type returnType, String id, List<Var> args, List<Var> vars, List<Stmt> stmtList) {
  public Method {
    this.args = Collections.unmodifiableList(args);
    this.vars = Collections.unmodifiableList(vars);
    this.stmtList = Collections.unmodifiableList(stmtList);
  }

  public Method(jlitec.parsetree.Method method) {
    this(
        Type.fromParseTree(method.type()),
        method.name().id(),
        method.args().stream().map(Var::new).collect(Collectors.toUnmodifiableList()),
        method.vars().stream().map(Var::new).collect(Collectors.toUnmodifiableList()),
        method.stmtList().stream()
            .map(Stmt::fromParseTree)
            .collect(Collectors.toUnmodifiableList()));
  }
}
