package jlitec.ast.expr;

public enum UnaryOp {
  NOT,
  NEGATIVE;

  public static UnaryOp fromParseTree(jlitec.parsetree.expr.UnaryOp op) {
    return switch (op) {
      case NEGATIVE -> NEGATIVE;
      case NOT -> NOT;
    };
  }
}
