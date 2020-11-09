package jlitec.backend.arm.insn;

import jlitec.backend.arm.Insn;

public record LabelInsn(String label) implements Insn {
  @Override
  public String print(int indent) {
    return label.replace("%", "") + ":";
  }
}
