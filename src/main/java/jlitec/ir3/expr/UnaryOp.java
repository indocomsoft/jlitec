package jlitec.ir3.expr;

import jlitec.Printable;

public enum UnaryOp implements Printable {
  NOT,
  NEGATIVE;

  @Override
  public String print(int indent) {
    return switch (this) {
      case NOT -> "!";
      case NEGATIVE -> "-";
    };
  }
}
