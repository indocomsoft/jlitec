package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

public record SDIVInsn(Condition condition, Register dst, Register src1, Register src2)
    implements ARMInsn {
  @Override
  public String print(int indent) {
    return "SDIV" + condition.print(0) + " " + dst.name() + ", " + src1.name() + ", " + src2.name();
  }
}
