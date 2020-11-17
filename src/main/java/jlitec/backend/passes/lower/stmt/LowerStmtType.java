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
  LDR_SPILL,
  STR_SPILL,
  MOV,
  RETURN,
  PUSH_STACK,
  POP_STACK,
  UNARY;
}
