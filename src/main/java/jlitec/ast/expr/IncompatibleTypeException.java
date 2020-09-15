package jlitec.ast.expr;

public class IncompatibleTypeException extends RuntimeException {
  /**
   * Constructor for binary expression.
   *
   * @param op the binary operator.
   * @param lhs the left operand.
   * @param rhs the right operand.
   */
  public IncompatibleTypeException(BinaryOp op, Expr lhs, Expr rhs) {
    super(
            new StringBuilder()
                    .append("Incompatible types of operands for binary operator `")
                    .append(op.toString())
                    .append("': the type of lhs `")
                    .append(lhs.print(0))
                    .append("' is ")
                    .append(lhs.getType().map(Enum::toString).orElse("UNKNOWN"))
                    .append(", while type of rhs `")
                    .append(rhs.print(0))
                    .append("' is ")
                    .append(rhs.getType().map(Enum::toString).orElse("UNKNOWN"))
                    .append('.')
                    .toString());
  }

  public IncompatibleTypeException(BinaryOp op, Expr lhs, Expr rhs, Expr.Type expectedLhs, Expr.Type expectedRhs) {
    super(
        new StringBuilder()
            .append("Incompatible types of operands for binary operator `")
            .append(op.toString())
            .append("': the type of lhs `")
            .append(lhs.print(0))
            .append("' is ")
            .append(lhs.getType().map(Enum::toString).orElse("UNKNOWN"))
            .append(" but expected ")
            .append(expectedLhs.toString())
            .append(", while type of rhs `")
            .append(rhs.print(0))
            .append("' encountered is ")
            .append(rhs.getType().map(Enum::toString).orElse("UNKNOWN"))
            .append(" but expected ")
            .append(expectedRhs.toString())
            .append('.')
            .toString());
  }

  /**
   * Constructor for unary expression.
   *
   * @param op the unary operator.
   * @param expr the expression.
   */
  public IncompatibleTypeException(UnaryOp op, Expr expr, Expr.Type expectedType) {
    super(
        new StringBuilder()
            .append("Incompatible types of operands for unary operator `")
            .append(op.toString())
            .append("': the type of the operand `")
            .append(expr.print(0))
            .append("' is ")
            .append(expr.getType().map(Enum::toString).orElse("UNKNOWN"))
                .append(", but expected ")
                .append(expectedType.toString())
                .append('.')
            .toString());
  }
}
