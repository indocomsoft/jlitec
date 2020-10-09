package jlitec.ir3.stmt;

import jlitec.Printable;

public interface Stmt extends Printable {
  StmtType getStmtType();
}
