package jlitec.ast.expr;

public enum BinaryOp {
  OR("||"),
  AND("&&"),
  GT(">"),
  LT("<"),
  GEQ(">="),
  LEQ("<="),
  EQ("=="),
  NEQ("!="),
  PLUS("+"),
  MINUS("-"),
  MULT("*"),
  DIV("/");

  private String representation;

  BinaryOp(String representation) {
    this.representation = representation;
  }

  @Override
  public String toString() {
    return representation;
  }
}
