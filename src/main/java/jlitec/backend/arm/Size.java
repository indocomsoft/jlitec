package jlitec.backend.arm;

import jlitec.Printable;

public enum Size implements Printable {
  WORD, /* WORD, the default, hence omitted */
  B, /* BYTE */
  SB, /* SIGNED BYTE */
  H, /* HALFWORD */
  SH, /* SIGNED HALFWORD */
  D; /* DOUBLEWORD */

  @Override
  public String print(int indent) {
    return switch (this) {
      case WORD -> "";
      case B, SB, H, SH, D -> this.name();
    };
  }
}
