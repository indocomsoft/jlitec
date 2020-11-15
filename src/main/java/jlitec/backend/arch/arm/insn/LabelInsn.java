package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.Insn;

public record LabelInsn(String label) implements Insn {
  @Override
  public String print(int indent) {
    return label.replace("%", "") + ":";
  }
}
