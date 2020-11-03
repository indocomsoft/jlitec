package jlitec.backend.arm;

public interface MemoryInsn extends ARMInsn {
  Size size();

  Register register();

  AddressingMode addressingMode();
}
