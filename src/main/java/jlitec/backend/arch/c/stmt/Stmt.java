package jlitec.backend.arch.c.stmt;

import jlitec.Printable;

public interface Stmt extends Printable {
  StmtType getStmtType();
}
