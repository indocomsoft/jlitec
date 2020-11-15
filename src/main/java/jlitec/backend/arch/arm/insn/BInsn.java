package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.ARMInsn;
import jlitec.backend.arch.arm.Condition;

public record BInsn(Condition condition, String label) implements ARMInsn {
  @Override
  public String print(int indent) {
    return "B" + condition.print(0) + " " + label.replace("%", "");
  }
}
