package jlitec.ast;

import java.util.List;
import jlitec.ast.stmt.Stmt;

public class Method {
  public final Type type;
  public final String id;
  public final List<Var> args;
  public final List<Var> vars;
  public final List<Stmt> stmtList;

  /**
   * The only constructor.
   *
   * @param type return type.
   * @param id method name.
   * @param args list of arguments.
   * @param vars list of variables.
   * @param stmtList list of statements as method body.
   */
  public Method(Type type, String id, List<Var> args, List<Var> vars, List<Stmt> stmtList) {
    this.type = type;
    this.id = id;
    this.args = args;
    this.vars = vars;
    this.stmtList = stmtList;
  }
}
