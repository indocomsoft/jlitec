package jlitec.ir3.expr;

import jlitec.Printable;

public enum BinaryOp implements Printable {
  LT,
  GT,
  LEQ,
  GEQ,
  EQ,
  NEQ,
  OR,
  AND,
  MULT,
  DIV,
  PLUS,
  MINUS;

  @Override
  public String print(int indent) {
    return switch (this) {
      case LT -> "<";
      case GT -> ">";
      case LEQ -> "<=";
      case GEQ -> ">=";
      case EQ -> "==";
      case NEQ -> "!=";
      case OR -> "||";
      case AND -> "&&";
      case MULT -> "*";
      case DIV -> "/";
      case PLUS -> "+";
      case MINUS -> "-";
    };
  }
}
