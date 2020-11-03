package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Operand2;
import jlitec.backend.arm.Register;

public record MOVInsn(Condition condition, Register register, Operand2 op2) implements ARMInsn {}
