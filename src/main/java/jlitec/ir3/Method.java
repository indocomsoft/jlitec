package jlitec.ir3;

import java.util.List;
import jlitec.ir3.stmt.Stmt;

public record Method(
    String cname,
    Type returnType,
    String id,
    List<Var> args,
    List<Var> vars,
    List<Stmt> stmtList) {}
