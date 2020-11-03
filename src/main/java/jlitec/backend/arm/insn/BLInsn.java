package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;

public record BLInsn(Condition condition, String label) implements ARMInsn {
  @Override
  public String print(int indent) {
    return "BL" + condition.print(0) + " " + label;
  }
}
