package jlitec.backend.arm.insn;

import jlitec.backend.arm.ARMInsn;
import jlitec.backend.arm.Condition;

public record BLInsn(Condition condition, String label) implements ARMInsn {}
