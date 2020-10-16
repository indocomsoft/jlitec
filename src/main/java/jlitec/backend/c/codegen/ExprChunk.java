package jlitec.backend.c.codegen;

import java.util.List;
import jlitec.backend.c.expr.Expr;
import jlitec.backend.c.stmt.Stmt;

public record ExprChunk(Expr expr, List<Stmt> stmtList) {}
