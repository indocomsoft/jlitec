package jlitec.backend.passes.lower.stmt;

import jlitec.Printable;

public interface LowerStmt extends Printable {
  LowerStmtType stmtExtensionType();
}
