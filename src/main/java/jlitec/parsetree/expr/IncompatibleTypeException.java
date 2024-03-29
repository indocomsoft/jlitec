package jlitec.parsetree.expr;

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
        "Incompatible types of operands for binary operator `"
            + op.toString()
            + "': the type of lhs `"
            + lhs.print(0)
            + "' is "
            + lhs.getTypeHint().map(Enum::toString).orElse("UNKNOWN")
            + ", while type of rhs `"
            + rhs.print(0)
            + "' is "
            + rhs.getTypeHint().map(Enum::toString).orElse("UNKNOWN")
            + '.');
  }

  /**
   * Constructor for binary expression with expected types.
   *
   * @param op the binary operator.
   * @param lhs the left operand.
   * @param rhs the right operand.
   * @param expectedLhs the expected type of the left operand.
   * @param expectedRhs the expected type of the right operand.
   */
  public IncompatibleTypeException(
      BinaryOp op, Expr lhs, Expr rhs, Expr.TypeHint expectedLhs, Expr.TypeHint expectedRhs) {
    super(
        "Incompatible types of operands for binary operator `"
            + op.toString()
            + "': the type of lhs `"
            + lhs.print(0)
            + "' is "
            + lhs.getTypeHint().map(Enum::toString).orElse("UNKNOWN")
            + " but expected "
            + expectedLhs.toString()
            + ", while type of rhs `"
            + rhs.print(0)
            + "' encountered is "
            + rhs.getTypeHint().map(Enum::toString).orElse("UNKNOWN")
            + " but expected "
            + expectedRhs.toString()
            + '.');
  }

  /**
   * Constructor for unary expression.
   *
   * @param op the unary operator.
   * @param expr the expression.
   */
  public IncompatibleTypeException(UnaryOp op, Expr expr, Expr.TypeHint expectedTypeHint) {
    super(
        "Incompatible types of operands for unary operator `"
            + op.toString()
            + "': the type of the operand `"
            + expr.print(0)
            + "' is "
            + expr.getTypeHint().map(Enum::toString).orElse("UNKNOWN")
            + ", but expected "
            + expectedTypeHint.toString()
            + '.');
  }
}
