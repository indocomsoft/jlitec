package jlitec.backend.arch.arm.codegen;

import java.util.Collections;
import java.util.List;
import jlitec.backend.arch.arm.Insn;
import jlitec.backend.arch.arm.Register;

public record GetRegChunk(List<Insn> insnList, Register x, Register y, Register z) {
  public GetRegChunk {
    this.insnList = Collections.unmodifiableList(insnList);
  }
}
