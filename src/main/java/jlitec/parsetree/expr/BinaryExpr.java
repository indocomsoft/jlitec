package jlitec.parsetree.expr;

import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;
import jlitec.Printable;

public record BinaryExpr(
    BinaryOp op,
    Expr lhs,
    Expr rhs,
    Optional<TypeHint> typeHint,
    Location leftLocation,
    Location rightLocation)
    implements Expr, Printable {
  /**
   * A constructor that will check the type validity, and synthesise the required type for the newly
   * created binary expression.
   *
   * @param op the binary operator.
   * @param lhs the left operand.
   * @param rhs the right operand.
   */
  public BinaryExpr(BinaryOp op, Expr lhs, Expr rhs, Location left, Location right) {
    this(
        op,
        lhs,
        rhs,
        switch (op) {
          case OR, AND -> {
            if (lhs.getTypeHint().filter(t -> t != TypeHint.BOOL).isPresent()
                || rhs.getTypeHint().filter(t -> t != TypeHint.BOOL).isPresent()) {
              throw new IncompatibleTypeException(op, lhs, rhs, TypeHint.BOOL, TypeHint.BOOL);
            }
            yield Optional.of(TypeHint.BOOL);
          }
          case GT, LT, GEQ, LEQ, EQ, NEQ -> {
            if (lhs.getTypeHint().filter(t -> t != TypeHint.INT).isPresent()
                || rhs.getTypeHint().filter(t -> t != TypeHint.INT).isPresent()) {
              throw new IncompatibleTypeException(op, lhs, rhs, TypeHint.INT, TypeHint.INT);
            }
            yield Optional.of(TypeHint.BOOL);
          }
          case MULT, DIV, MINUS -> {
            if (lhs.getTypeHint().filter(t -> t != TypeHint.INT).isPresent()
                || rhs.getTypeHint().filter(t -> t != TypeHint.INT).isPresent()) {
              throw new IncompatibleTypeException(op, lhs, rhs, TypeHint.INT, TypeHint.INT);
            }
            yield Optional.of(TypeHint.INT);
          }
          case PLUS -> {
            if (lhs.getTypeHint().isEmpty() && rhs.getTypeHint().isEmpty()) {
              yield Optional.empty();
            } else if (lhs.getTypeHint().filter(t -> t != TypeHint.INT).isEmpty()
                && rhs.getTypeHint().filter(t -> t != TypeHint.INT).isEmpty()) {
              yield Optional.of(TypeHint.INT);
            } else if (lhs.getTypeHint().filter(t -> t != TypeHint.STRING).isEmpty()
                && rhs.getTypeHint().filter(t -> t != TypeHint.STRING).isEmpty()) {
              yield Optional.of(TypeHint.STRING);
            } else {
              throw new IncompatibleTypeException(op, lhs, rhs);
            }
          }
        },
        left,
        right);
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }

  @Override
  public Optional<TypeHint> getTypeHint() {
    return typeHint;
  }

  @Override
  public String print(int indent) {
    return lhs.print(indent) + ' ' + op.toString() + ' ' + rhs.print(indent);
  }
}
