package jlitec.backend.passes.lower.stmt;

public enum LowerStmtType {
  BINARY,
  BRANCH_LINK,
  CMP,
  FIELD_ACCESS,
  FIELD_ASSIGN,
  GOTO,
  IMMEDIATE,
  LABEL,
  LOAD_STACK_ARG,
  LDR_SPILL,
  STR_SPILL,
  MOV,
  RETURN,
  PUSH_PAD_STACK,
  PUSH_STACK,
  POP_STACK,
  REG_BINARY,
  UNARY;
}
