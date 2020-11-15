package jlitec.backend.arch.arm;

public interface CompareInsn extends ARMInsn {
  Register register();

  Operand2 op2();

  Type type();

  enum Type {
    CMN,
    CMP,
    TEQ,
    TST;
  }

  @Override
  default String print(int indent) {
    return type().name() + condition().print(0) + " " + register().name() + ", " + op2().print(0);
  }
}
