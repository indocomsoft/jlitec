package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.MemoryAddress;
import jlitec.backend.arch.arm.MemoryInsn;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.arch.arm.Size;

public record STRInsn(
    Condition condition, Size size, Register register, MemoryAddress memoryAddress)
    implements MemoryInsn {
  public STRInsn {
    if (!isValidSize(size)) {
      throw new RuntimeException("Invalid size for STR: `" + size.print(0) + "'");
    }
    if (!isValidMemoryAddress(memoryAddress)) {
      throw new RuntimeException("Cannot use PC-relative memory address for STR");
    }
  }

  @Override
  public Type type() {
    return Type.STR;
  }

  public static boolean isValidSize(Size size) {
    return switch (size) {
      case WORD, B, H, D -> true;
      case SB, SH -> false;
    };
  }

  private static boolean isValidMemoryAddress(MemoryAddress memoryAddress) {
    return !(memoryAddress instanceof MemoryAddress.PCRelative);
  }
}
