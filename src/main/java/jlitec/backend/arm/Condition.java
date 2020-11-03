package jlitec.backend.arm;

public enum Condition {
  EQ, /* Equal */
  NE, /* Not equal */
  GE, /* Signed greater than or equal */
  LT, /* Signed less than */
  GT, /* Signed greater than */
  LE, /* Signed less than or equal */
  AL /* Always (usually omitted) */;
}
