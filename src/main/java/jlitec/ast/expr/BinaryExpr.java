package jlitec.ast.expr;

import jlitec.ast.Printable;

import java.util.Optional;

public record BinaryExpr(BinaryOp op, Expr lhs, Expr rhs, Optional<Type> type) implements Expr, Printable {
  public BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) {
    this(op, lhs, rhs, switch (op) {
      case OR, AND, GT, LT, GEQ, LEQ, EQ, NEQ -> {
        if (lhs.getType().filter(t -> t != Type.BOOL).isPresent() || rhs.getType().filter(t -> t != Type.BOOL).isPresent()) {
          throw new IncompatibleTypeException(op, lhs, rhs);
        }
        yield Optional.of(Type.BOOL);
      }
      case MULT, DIV, MINUS -> {
        if (lhs.getType().filter(t -> t != Type.INT).isPresent() || rhs.getType().filter(t -> t != Type.INT).isPresent()) {
          throw new IncompatibleTypeException(op, lhs, rhs);
        }
        yield Optional.of(Type.INT);
      }
      case PLUS -> {
        if (lhs.getType().isEmpty() && rhs.getType().isEmpty()) {
          yield Optional.empty();
        } else if (lhs.getType().filter(t -> t != Type.INT).isEmpty() && rhs.getType().filter(t -> t != Type.INT).isEmpty()) {
          yield Optional.of(Type.INT);
        } else if (lhs.getType().filter(t -> t != Type.STRING).isEmpty() && rhs.getType().filter(t -> t != Type.STRING).isEmpty()) {
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
