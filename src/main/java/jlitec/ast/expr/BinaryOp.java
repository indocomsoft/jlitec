package jlitec.ast.expr;

public enum BinaryOp {
  OR,
  AND,
  GT,
  LT,
  GEQ,
  LEQ,
  EQ,
  NEQ,
  PLUS,
  MINUS,
  MULT,
  DIV;

  public static BinaryOp fromParseTree(jlitec.parsetree.expr.BinaryOp op) {
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
}
