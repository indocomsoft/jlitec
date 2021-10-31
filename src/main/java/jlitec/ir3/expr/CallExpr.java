package jlitec.ir3.expr;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.RvalExpr;

public record CallExpr(IdRvalExpr target, List<RvalExpr> args) implements Expr {
  public CallExpr {
    args = Collections.unmodifiableList(args);
  }

  @Override
  public ExprType getExprType() {
    return ExprType.CALL;
  }

  @Override
  public String print(int indent) {
    return target.print(indent)
        + "("
        + args.stream().map(v -> v.print(indent)).collect(Collectors.joining(", "))
        + ")";
  }
}
