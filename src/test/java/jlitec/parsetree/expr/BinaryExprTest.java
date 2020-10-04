package jlitec.parsetree.expr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java_cup.runtime.ComplexSymbolFactory.Location;
import org.junit.jupiter.api.Test;

public class BinaryExprTest {
  private record Argument(Expr lhs, Expr rhs) {}

  private Location location = new Location(0, 0, 0);

  @Test
  public void testBooleanInputBooleanOutputAcceptable() {
    final List<Argument> list =
        List.of(
            // Unknown || Unknown
            new Argument(new IdExpr("i", location, location), new IdExpr("a", location, location)),
            new Argument(
                new ParenExpr(new IntLiteralExpr(5, location, location), location, location),
                new IdExpr("a", location, location)), // type-erasure of parenthesis
            // Bool || Unknown
            new Argument(
                new BoolLiteralExpr(true, location, location), new IdExpr("a", location, location)),
            // Unknown || Bool
            new Argument(
                new IdExpr("i", location, location), new BoolLiteralExpr(true, location, location)),
            new Argument(
                new IdExpr("i", location, location),
                new UnaryExpr(
                    UnaryOp.NOT,
                    new BoolLiteralExpr(false, location, location),
                    location,
                    location)),
            // Bool || Bool
            new Argument(
                new BoolLiteralExpr(true, location, location),
                new BoolLiteralExpr(false, location, location)),
            new Argument(
                new BinaryExpr(
                    BinaryOp.AND,
                    new BoolLiteralExpr(true, location, location),
                    new BoolLiteralExpr(false, location, location),
                    location,
                    location),
                new BoolLiteralExpr(false, location, location)));
    for (final var arg : list) {
      assertDoesNotThrow(
          () -> {
            final var bexpr = new BinaryExpr(BinaryOp.OR, arg.lhs(), arg.rhs(), location, location);
            assertEquals(bexpr.getTypeHint(), Optional.of(Expr.TypeHint.BOOL));
          });
      assertDoesNotThrow(
          () -> {
            final var bexpr =
                new BinaryExpr(BinaryOp.AND, arg.lhs(), arg.rhs(), location, location);
            assertEquals(bexpr.getTypeHint(), Optional.of(Expr.TypeHint.BOOL));
          });
    }
  }

  @Test
  public void testBooleanInputBooleanOutputThrows() {
    final List<Argument> list =
        List.of(
            // Int || Unknown
            new Argument(
                new IntLiteralExpr(5, location, location), new IdExpr("a", location, location)),
            new Argument(
                new IdExpr("a", location, location), new IntLiteralExpr(5, location, location)),
            // String || Unknown
            new Argument(
                new StringLiteralExpr("i", location, location),
                new IdExpr("a", location, location)),
            new Argument(
                new IdExpr("a", location, location),
                new StringLiteralExpr("i", location, location)),
            // Int || Bool
            new Argument(
                new IntLiteralExpr(5, location, location),
                new BoolLiteralExpr(true, location, location)),
            new Argument(
                new BoolLiteralExpr(true, location, location),
                new IntLiteralExpr(5, location, location)),
            // String || Bool
            new Argument(
                new StringLiteralExpr("true", location, location),
                new BoolLiteralExpr(false, location, location)),
            new Argument(
                new BoolLiteralExpr(true, location, location),
                new StringLiteralExpr("false", location, location)));
    for (final var arg : list) {
      assertThrows(
          IncompatibleTypeException.class,
          () -> new BinaryExpr(BinaryOp.OR, arg.lhs(), arg.rhs(), location, location));
      assertThrows(
          IncompatibleTypeException.class,
          () -> new BinaryExpr(BinaryOp.AND, arg.lhs(), arg.rhs(), location, location));
    }
  }
}
