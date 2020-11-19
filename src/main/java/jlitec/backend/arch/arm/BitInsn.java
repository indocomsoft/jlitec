package jlitec.backend.arch.arm;

import jlitec.Printable;
import jlitec.backend.passes.lower.stmt.BitOp;

public interface BitInsn extends ARMInsn {
  Register dst();

  Register src();

  Rssh shift();

  BitOp op();

  @Override
  default String print(int indent) {
    return op().name()
        + condition().print(0)
        + " "
        + dst().name()
        + ", "
        + src().name()
        + ", "
        + shift().print(0);
  }

  interface Rssh extends Printable {
    record Reg(Register reg) implements Rssh {
      @Override
      public String print(int indent) {
        return reg.name();
      }
    }

    record Immediate(int value) implements Rssh {
      @Override
      public String print(int indent) {
        return "#" + value;
      }
    }
  }
}
