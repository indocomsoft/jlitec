package jlitec.backend.c.stmt;

import jlitec.Printable;

public interface Stmt extends Printable {
  StmtType getStmtType();
}
