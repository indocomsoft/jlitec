package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Operand2;
import jlitec.backend.arm.Register;

public record MVNInsn(Condition condition, Register register, Operand2 op2) implements ARMInsn {
  @Override
  public String print(int indent) {
    return "MVN" + condition.print(0) + " " + register.name() + ", " + op2.print(0);
  }
}
