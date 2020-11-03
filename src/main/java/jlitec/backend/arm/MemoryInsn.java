package jlitec.backend.arm;

public interface MemoryInsn extends ARMInsn {
  Size size();

  Register register();

  MemoryAddress memoryAddress();

  Type type();

  enum Type {
    STR,
    LDR;
  }

  @Override
  default String print(int indent) {
    return type().name()
        + size().print(0)
        + " "
        + register().name()
        + ", "
        + memoryAddress().print(0);
  }
}
