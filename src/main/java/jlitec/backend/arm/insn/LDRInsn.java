package jlitec.backend.arm.insn;

import jlitec.backend.arm.AddressingMode;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.MemoryInsn;
import jlitec.backend.arm.Register;
import jlitec.backend.arm.Size;

public record LDRInsn(
    Condition condition, Size size, Register register, AddressingMode addressingMode)
    implements MemoryInsn {}
