package jlitec.ast.expr;

import java.util.Optional;

public class IncompatibleTypeException extends RuntimeException {
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
      .toString()
    );
  }

  public IncompatibleTypeException(UnaryOp op, Expr expr) {
    super (
            new StringBuilder()
                    .append("Incompatible types of operands for unary operator `")
                    .append(op.toString())
                    .append("': the type of the operand `")
                    .append(expr.print(0))
                    .append("' is ")
                    .append(expr.getType().map(Enum::toString).orElse("UNKNOWN"))
            .toString()
    );
  }
}
