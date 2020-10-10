package jlitec.ir3.codegen;

import jlitec.ir3.stmt.LabelStmt;

public class LabelGen {
  private int counter = 1;

  public LabelStmt gen() {
    return new LabelStmt("L" + (counter++));
  }
}
