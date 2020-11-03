package jlitec.backend.arm.insn;

import jlitec.backend.arm.Condition;
import jlitec.backend.arm.MemoryAddress;
import jlitec.backend.arm.MemoryInsn;
import jlitec.backend.arm.Register;
import jlitec.backend.arm.Size;

public record LDRInsn(
    Condition condition, Size size, Register register, MemoryAddress memoryAddress)
    implements MemoryInsn {
  @Override
  public Type type() {
    return Type.LDR;
  }
}
