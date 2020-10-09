package jlitec.ir3.codegen;

import java.util.List;
import jlitec.ir3.expr.Expr;
import jlitec.ir3.stmt.Stmt;

public record ExprChunk(Expr expr, List<Stmt> stmtList) {}
