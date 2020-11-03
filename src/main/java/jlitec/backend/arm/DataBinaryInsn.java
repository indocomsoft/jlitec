package jlitec.backend.arm;

public interface DataBinaryInsn extends ARMInsn {
  boolean updateConditionFlags();

  Register dst();

  Register src();

  Operand2 op2();
}
