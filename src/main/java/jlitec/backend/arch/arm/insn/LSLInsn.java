package jlitec.backend.arch.arm.insn;

import jlitec.backend.arch.arm.BitInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.lower.stmt.BitOp;

public record LSLInsn(Condition condition, Register dst, Register src, BitInsn.Rssh shift)
    implements BitInsn {
  @Override
  public BitOp op() {
    return BitOp.LSL;
  }
}
