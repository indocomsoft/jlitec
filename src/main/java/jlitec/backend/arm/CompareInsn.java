package jlitec.backend.arm;

public interface CompareInsn {
  Register register();

  Operand2 op2();
}
