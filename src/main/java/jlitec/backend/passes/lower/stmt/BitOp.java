package jlitec.backend.passes.lower.stmt;

public enum BitOp {
  ASR, // arithmetic shift right (use the highest bit to "sign-extend")
  LSL, // logical shift left
  LSR, // logical shift right (use 0 as the highest bit)
  ROR; // rotate right
}
