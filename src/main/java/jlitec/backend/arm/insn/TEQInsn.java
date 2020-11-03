package jlitec.backend.arm.insn;

import jlitec.backend.arm.CompareInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Operand2;
import jlitec.backend.arm.Register;

/** Update CPSR flags on Rn EOR Operand2. */
public record TEQInsn(Condition condition, Register register, Operand2 op2) implements CompareInsn {
  @Override
  public Type type() {
    return Type.TEQ;
  }
}
