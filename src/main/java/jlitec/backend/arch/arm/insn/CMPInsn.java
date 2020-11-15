package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.CompareInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Register;

/** Update CPSR flags on Rn – Operand2. */
public record CMPInsn(Condition condition, Register register, Operand2 op2) implements CompareInsn {
  @Override
  public Type type() {
    return Type.CMP;
  }
}
