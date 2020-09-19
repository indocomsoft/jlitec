package jlitec.ast.expr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryExprTest {
  private record Argument(Expr lhs, Expr rhs) { }

  @Test
  public void testBooleanInputBooleanOutputAcceptable() {
    final List<Argument> list = List.of(
            // Unknown || Unknown
            new Argument(new IdExpr("i"), new IdExpr("a")),
            new Argument(new ParenExpr(new IntLiteralExpr(5)), new IdExpr("a")), // type-erasure of parenthesis
            // Bool || Unknown
            new Argument(new BoolLiteralExpr(true), new IdExpr("a")),
            // Unknown || Bool
            new Argument(new IdExpr("i"), new BoolLiteralExpr(true)),
            new Argument(new IdExpr("i"), new UnaryExpr(UnaryOp.NOT, new BoolLiteralExpr(false))),
            // Bool || Bool
            new Argument(new BoolLiteralExpr(true), new BoolLiteralExpr(false)),
            new Argument(new BinaryExpr(BinaryOp.AND, new BoolLiteralExpr(true), new BoolLiteralExpr(false)), new BoolLiteralExpr(false))
    );
    for (final var arg: list) {
      assertDoesNotThrow(() -> {
        final var bexpr = new BinaryExpr(BinaryOp.OR, arg.lhs(), arg.rhs());
        assertEquals(bexpr.getType(), Optional.of(Expr.Type.BOOL));
      });
      assertDoesNotThrow(() -> {
        final var bexpr = new BinaryExpr(BinaryOp.AND, arg.lhs(), arg.rhs());
        assertEquals(bexpr.getType(), Optional.of(Expr.Type.BOOL));
      });
    }
  }

  @Test
  public void testBooleanInputBooleanOutputThrows() {
    final List<Argument> list = List.of(
            // Int || Unknown
            new Argument(new IntLiteralExpr(5), new IdExpr("a")),
            new Argument(new IdExpr("a"), new IntLiteralExpr(5)),
            // String || Unknown
            new Argument(new StringLiteralExpr("i"), new IdExpr("a")),
            new Argument(new IdExpr("a"), new StringLiteralExpr("i")),
            // Int || Bool
            new Argument(new IntLiteralExpr(5), new BoolLiteralExpr(true)),
            new Argument(new BoolLiteralExpr(true), new IntLiteralExpr(5)),
            // String || Bool
            new Argument(new StringLiteralExpr("true"), new BoolLiteralExpr(false)),
            new Argument(new BoolLiteralExpr(true), new StringLiteralExpr("false"))
    );
    for (final var arg: list) {
      assertThrows(IncompatibleTypeException.class, () -> new BinaryExpr(BinaryOp.OR, arg.lhs(), arg.rhs()));
      assertThrows(IncompatibleTypeException.class, () -> new BinaryExpr(BinaryOp.AND, arg.lhs(), arg.rhs()));
    }
  }
}
