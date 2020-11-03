package jlitec.backend.arm.insn;

import java.util.Set;
import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

/*
 Not implemented:
   - non-AL condition code
*/
public record STMFDInsn(Set<Register> registers) implements ARMInsn {
  @Override
  public Condition condition() {
    return Condition.AL;
  }
}
