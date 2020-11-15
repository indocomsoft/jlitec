package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;
import org.apache.commons.text.StringEscapeUtils;

public record StringLiteralExpr(String value, Location leftLocation, Location rightLocation)
    implements Expr {
  public StringLiteralExpr {
    if (value.isEmpty()) {
      throw new RuntimeException("String Literal cannot be \"\"");
    }
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_STRING_LITERAL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.of(TypeHint.STRING);
  }

  @Override
  public String print(int indent) {
    return '"' + StringEscapeUtils.escapeJava(value) + '"';
  }
}
