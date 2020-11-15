package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.MemoryAddress;
import jlitec.backend.arch.arm.MemoryInsn;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.arch.arm.Size;

public record LDRInsn(
    Condition condition, Size size, Register register, MemoryAddress memoryAddress)
    implements MemoryInsn {
  @Override
  public Type type() {
    return Type.LDR;
  }
}
