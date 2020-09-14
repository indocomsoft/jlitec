package jlitec.ast.expr;

public enum UnaryOp {
  NOT("!"),
  NEGATIVE("-");

  private String representation;

  UnaryOp(String representation) {
    this.representation = representation;
  }

  @Override
  public String toString() {
    return representation;
  }
}
