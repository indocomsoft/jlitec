package jlitec.backend.arm.insn;

import jlitec.backend.arm.Condition;
import jlitec.backend.arm.MemoryAddress;
import jlitec.backend.arm.MemoryInsn;
import jlitec.backend.arm.Register;
import jlitec.backend.arm.Size;

public record STRInsn(
    Condition condition, Size size, Register register, MemoryAddress memoryAddress)
    implements MemoryInsn {
  public STRInsn {
    if (!isValid(size)) {
      throw new RuntimeException("Invalid size for STR: `" + size.print(0) + "'");
    }
  }

  @Override
  public Type type() {
    return Type.STR;
  }

  public static boolean isValid(Size size) {
    return switch (size) {
      case WORD, B, H, D -> true;
      case SB, SH -> false;
    };
  }
}
