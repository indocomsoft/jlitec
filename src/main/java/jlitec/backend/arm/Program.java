package jlitec.backend.arm;

import java.util.Collections;
import java.util.List;

public record Program(List<Insn> insnList) {
  public Program {
    this.insnList = Collections.unmodifiableList(insnList);
  }
}
