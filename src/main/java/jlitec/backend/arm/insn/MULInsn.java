package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Register;

public record MULInsn(Condition condition, Register dst, Register src1, Register src2)
    implements ARMInsn {}
