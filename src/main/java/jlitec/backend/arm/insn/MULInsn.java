package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

public record MULInsn(
    Condition condition, boolean updateConditionFlags, Register dst, Register src1, Register src2)
    implements ARMInsn {
  @Override
  public String print(int indent) {
    return "MUL"
        + condition.print(0)
        + (updateConditionFlags ? "S" : "")
        + " "
        + dst.name()
        + ", "
        + src1.name()
        + ", "
        + src2.name();
  }
}
