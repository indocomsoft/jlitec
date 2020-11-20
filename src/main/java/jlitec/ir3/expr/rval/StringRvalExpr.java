package jlitec.ir3.expr.rval;

import org.apache.commons.text.StringEscapeUtils;

public record StringRvalExpr(String value) implements LiteralRvalExpr {
  @Override
  public RvalExprType getRvalExprType() {
    return RvalExprType.STRING;
  }

  @Override
  public String print(int indent) {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
  }
}
