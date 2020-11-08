package jlitec.backend.arm.codegen;

import jlitec.backend.arm.Insn;
import jlitec.backend.arm.Register;

import java.util.Collections;
import java.util.List;

public record GetRegChunk(List<Insn> insnList, Register x, Register y, Register z) {
  public GetRegChunk {
    this.insnList = Collections.unmodifiableList(insnList);
  }
}
