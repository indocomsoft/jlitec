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

  public static BinaryOp fromAst(jlitec.ast.expr.BinaryOp op) {
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

  public static BinaryOp fromAstOpposite(jlitec.ast.expr.BinaryOp op) {
    return switch (op) {
      case GT -> LEQ;
      case LT -> GEQ;
      case GEQ -> LT;
      case LEQ -> GT;
      case EQ -> NEQ;
      case NEQ -> EQ;
      case PLUS, MINUS, MULT, DIV, OR, AND -> throw new RuntimeException(
          "Invalid operator to get opposite");
    };
  }

  /**
   * For changing LHS <-> RHS
   *
   * @return
   */
  public BinaryOp comparisonFlip() {
    return switch (this) {
      case EQ, NEQ, PLUS, MINUS, MULT, DIV, OR, AND -> throw new RuntimeException();
      case LT -> GT;
      case GT -> LT;
      case GEQ -> LEQ;
      case LEQ -> GEQ;
    };
  }

  public BinaryOp comparisonOpposite() {
    return switch (this) {
      case LT -> GEQ;
      case GT -> LEQ;
      case LEQ -> GT;
      case GEQ -> LT;
      case EQ -> EQ;
      case NEQ -> NEQ;
      case PLUS, MINUS, MULT, DIV, OR, AND -> throw new RuntimeException(
          "Not a comparison operator");
    };
  }
}
