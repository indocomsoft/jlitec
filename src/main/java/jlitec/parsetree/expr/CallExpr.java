package jlitec.parsetree.expr;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record CallExpr(Expr target, List<Expr> args, Location leftLocation, Location rightLocation)
    implements Expr {
  public CallExpr {
    this.args = Collections.unmodifiableList(args);
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_CALL;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return Optional.empty();
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder().append(target.print(indent)).append('(');
    sb.append(args.stream().map(arg -> arg.print(indent)).collect(Collectors.joining(", ")));
    sb.append(')');
    return sb.toString();
  }
}
