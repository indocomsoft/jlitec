package jlitec.backend.arch.arm;

import jlitec.Printable;
import jlitec.backend.passes.lower.stmt.BitOp;

public interface Operand2 extends Printable {
  record Register(jlitec.backend.arch.arm.Register reg, BitOp op, BitInsn.Rssh shift)
      implements Operand2 {
    public Register(jlitec.backend.arch.arm.Register reg) {
      this(reg, BitOp.LSL, new BitInsn.Rssh.Immediate(0));
    }

    public boolean isPlain() {
      return op == BitOp.LSL && shift instanceof BitInsn.Rssh.Immediate imm && imm.value() == 0;
    }

    @Override
    public String print(int indent) {
      if (op == BitOp.LSL && shift instanceof BitInsn.Rssh.Immediate imm && imm.value() == 0) {
        return reg.name();
      }
      return reg.name() + ", " + op.name() + " " + shift.print(0);
    }
  }

  record Immediate(int value) implements Operand2 {
    @Override
    public String print(int indent) {
      return "#" + value;
    }
  }
}
