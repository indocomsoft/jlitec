package jlitec.ast.expr;

import java.util.Optional;
import jlitec.ast.Printable;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs, Optional<Type> type)
    implements Expr, Printable {
  /**
   * A constructor that will check the type validity, and synthesise the required type for the newly
   * created binary expression.
   *
   * @param op the binary operator.
   * @param lhs the left operand.
   * @param rhs the right operand.
   */
  public BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) {
    this(
        op,
        lhs,
        rhs,
        switch (op) {
          case OR, AND -> {
            if (lhs.getType().filter(t -> t != Type.BOOL).isPresent()
                || rhs.getType().filter(t -> t != Type.BOOL).isPresent()) {
              throw new IncompatibleTypeException(op, lhs, rhs, Type.BOOL, Type.BOOL);
            }
            yield Optional.of(Type.BOOL);
          }
          case GT, LT, GEQ, LEQ, EQ, NEQ -> {
            if (lhs.getType().filter(t -> t != Type.INT).isPresent()
                    || rhs.getType().filter(t -> t != Type.INT).isPresent()) {
              throw new IncompatibleTypeException(op, lhs, rhs, Type.INT, Type.INT);
            }
            yield Optional.of(Type.BOOL);
          }
          case MULT, DIV, MINUS -> {
            if (lhs.getType().filter(t -> t != Type.INT).isPresent()
                || rhs.getType().filter(t -> t != Type.INT).isPresent()) {
              throw new IncompatibleTypeException(op, lhs, rhs, Type.INT, Type.INT);
            }
            yield Optional.of(Type.INT);
          }
          case PLUS -> {
            if (lhs.getType().isEmpty() && rhs.getType().isEmpty()) {
              yield Optional.empty();
            } else if (lhs.getType().filter(t -> t != Type.INT).isEmpty()
                && rhs.getType().filter(t -> t != Type.INT).isEmpty()) {
              yield Optional.of(Type.INT);
            } else if (lhs.getType().filter(t -> t != Type.STRING).isEmpty()
                && rhs.getType().filter(t -> t != Type.STRING).isEmpty()) {
              yield Optional.of(Type.STRING);
            } else {
              throw new IncompatibleTypeException(op, lhs, rhs);
            }
          }
        });
  }

  @Override
  public ExprType getExprType() {
    return ExprType.EXPR_BINARY;
  }

  @Override
  public Optional<Type> getType() {
    return type;
  }

  @Override
  public String print(int indent) {
    return new StringBuilder()
        .append(lhs.print(indent))
        .append(' ')
        .append(op.toString())
        .append(' ')
        .append(rhs.print(indent))
        .toString();
  }
}
