package jlitec.backend.c.expr;

public enum ExprType {
  BOOL_LITERAL,
  INT_LITERAL,
  STRING_LITERAL,
  ID,
  BINARY,
  UNARY,
  FIELD,
  CALL,
  ADDR,
  NULL;
}
