package jlitec.backend.c.expr;

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

  public static BinaryOp fromIr3(jlitec.ir3.expr.BinaryOp op) {
    return switch (op) {
      case OR -> OR;
      case AND -> AND;
      case GT -> GT;
      case LT -> LT;
      case GEQ -> GEQ;
      case LEQ -> LEQ;
      case EQ -> EQ;
      case NEQ -> NEQ;
      case PLUS -> PLUS;
      case MINUS -> MINUS;
      case MULT -> MULT;
      case DIV -> DIV;
    };
  }

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
