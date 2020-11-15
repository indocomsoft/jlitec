package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.ARMInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Register;

public record MVNInsn(Condition condition, Register register, Operand2 op2) implements ARMInsn {
  @Override
  public String print(int indent) {
    return "MVN" + condition.print(0) + " " + register.name() + ", " + op2.print(0);
  }
}
