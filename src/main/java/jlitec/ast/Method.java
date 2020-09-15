package jlitec.ast;

import java.util.List;
import jlitec.ast.stmt.Stmt;

public record Method(Type type, String id, List<Var> args, List<Var> vars, List<Stmt> stmtList) { }
