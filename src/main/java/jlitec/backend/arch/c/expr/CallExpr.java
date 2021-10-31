package jlitec.backend.arch.c.expr;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record CallExpr(String methodName, List<Expr> args) implements Expr {
  public CallExpr {
    args = Collections.unmodifiableList(args);
  }

  @Override
  public ExprType getExprType() {
    return ExprType.CALL;
  }

  @Override
  public String print(int indent) {
    return methodName
        + "("
        + args.stream().map(e -> e.print(indent)).collect(Collectors.joining(", "))
        + ")";
  }
}
