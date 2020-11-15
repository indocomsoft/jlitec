package jlitec.backend.arch.c.codegen;

import java.util.List;
import jlitec.backend.arch.c.expr.Expr;
import jlitec.backend.arch.c.stmt.Stmt;

public record ExprChunk(Expr expr, List<Stmt> stmtList) {}
