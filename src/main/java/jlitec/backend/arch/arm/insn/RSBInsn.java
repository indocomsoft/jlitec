package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.DataBinaryInsn;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Register;

public record RSBInsn(
    Condition condition, boolean updateConditionFlags, Register dst, Register src, Operand2 op2)
    implements DataBinaryInsn {
  @Override
  public Type type() {
    return Type.RSB;
  }
}
