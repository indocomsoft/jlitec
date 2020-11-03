package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

public record BXInsn(Condition condition, Register register) implements ARMInsn {
  @Override
  public String print(int indent) {
    return "BX" + condition.print(0) + " " + register.name();
  }
}
