package jlitec.parsetree.expr;

public enum ExprType {
  // Literals
  EXPR_INT_LITERAL,
  EXPR_STRING_LITERAL,
  EXPR_BOOL_LITERAL,

  // Operations
  EXPR_BINARY,
  EXPR_UNARY,

  // Atom
  EXPR_DOT,
  EXPR_CALL,
  EXPR_THIS,
  EXPR_ID,
  EXPR_NEW,
  EXPR_NULL,

  // Wrapper
  EXPR_PAREN,
}
