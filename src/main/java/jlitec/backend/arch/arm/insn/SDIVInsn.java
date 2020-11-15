package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.ARMInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Register;

public record SDIVInsn(Condition condition, Register dst, Register src1, Register src2)
    implements ARMInsn {
  @Override
  public String print(int indent) {
    return "SDIV" + condition.print(0) + " " + dst.name() + ", " + src1.name() + ", " + src2.name();
  }
}
