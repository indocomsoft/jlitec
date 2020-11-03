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
public record STMFDInsn(Register register, EnumSet<Register> registers, boolean writeback)
    implements ARMInsn {
  @Override
  public Condition condition() {
    return Condition.AL;
  }

  @Override
  public String print(int indent) {
    final var regs = registers.stream().map(Enum::name).collect(Collectors.joining(", "));
    return "STMFD " + register + (writeback ? "!" : "") + ", {" + regs + "}";
  }
}
