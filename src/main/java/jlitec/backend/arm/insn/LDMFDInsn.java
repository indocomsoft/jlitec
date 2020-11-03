package jlitec.backend.arm.insn;

import java.util.Set;
import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

/*
 Not implemented:
   - non-AL condition code
*/
public record LDMFDInsn(Set<Register> register) implements ARMInsn {
  @Override
  public Condition condition() {
    return Condition.AL;
  }
}
