package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.ARMInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Register;

public record LSLInsn(Condition condition, Register dst, Register src, Operand2 shift)
    implements ARMInsn {
  @Override
  public String print(int indent) {
    return "LSL"
        + condition.print(0)
        + " "
        + dst.name()
        + ", "
        + src.name()
        + ", "
        + shift.print(0);
  }
}
