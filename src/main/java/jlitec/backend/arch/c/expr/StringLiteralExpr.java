package jlitec.backend.arch.c.expr;

import org.apache.commons.text.StringEscapeUtils;

public record StringLiteralExpr(String value) implements Expr {
  @Override
  public ExprType getExprType() {
    return ExprType.STRING_LITERAL;
  }

  @Override
  public String print(int indent) {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
  }
}
