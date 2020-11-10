package jlitec.backend.arm.insn;

import java.util.EnumSet;
import java.util.stream.Collectors;
import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

/*
 Not implemented:
   - non-AL condition code
*/
public record LDMFDInsn(Register register, EnumSet<Register> registers, boolean writeback)
    implements ARMInsn {
  public LDMFDInsn {
    if ((registers.size() & 1) == 1) {
      throw new RuntimeException(
          "Stack must be 8-byte aligned, instead LDMFD received "
              + (registers.size())
              + " registers.");
    }
  }

  @Override
  public Condition condition() {
    return Condition.AL;
  }

  @Override
  public String print(int indent) {
    final var regs = registers.stream().map(Enum::name).collect(Collectors.joining(", "));
    return "LDMFD " + register + (writeback ? "!" : "") + ", {" + regs + "}";
  }
}
