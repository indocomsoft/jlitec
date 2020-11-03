package jlitec.backend.arm.insn;

import jlitec.backend.arm.Condition;
import jlitec.backend.arm.DataBinaryInsn;
import jlitec.backend.arm.Operand2;
import jlitec.backend.arm.Register;

public record RSBInsn(
    Condition condition, boolean updateConditionFlags, Register dst, Register src, Operand2 op2)
    implements DataBinaryInsn {}
