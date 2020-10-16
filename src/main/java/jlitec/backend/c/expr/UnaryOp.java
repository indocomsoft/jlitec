package jlitec.backend.c.expr;

import jlitec.Printable;

public enum UnaryOp implements Printable {
  NOT,
  NEGATIVE;

  public static UnaryOp fromIr3(jlitec.ir3.expr.UnaryOp op) {
    return switch (op) {
      case NEGATIVE -> NEGATIVE;
      case NOT -> NOT;
    };
  }

  @Override
  public String print(int indent) {
    return switch (this) {
      case NEGATIVE -> "-";
      case NOT -> "!";
    };
  }
}
