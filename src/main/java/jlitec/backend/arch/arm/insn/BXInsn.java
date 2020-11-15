package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.ARMInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Register;

public record BXInsn(Condition condition, Register register) implements ARMInsn {
  @Override
  public String print(int indent) {
    return "BX" + condition.print(0) + " " + register.name();
  }
}
