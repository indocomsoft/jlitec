package jlitec.backend.arm;

import jlitec.Printable;

public enum Condition implements Printable {
  EQ, /* Equal */
  NE, /* Not equal */
  GE, /* Signed greater than or equal */
  LT, /* Signed less than */
  GT, /* Signed greater than */
  LE, /* Signed less than or equal */
  AL; /* Always (usually omitted) */

  @Override
  public String print(int indent) {
    return switch (this) {
      case EQ -> "EQ";
      case NE -> "NE";
      case GE -> "GE";
      case LT -> "LT";
      case GT -> "GT";
      case LE -> "LE";
      case AL -> "";
    };
  }
}
