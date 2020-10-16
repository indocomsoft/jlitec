package jlitec.backend.c.expr;

import jlitec.Printable;

public interface Expr extends Printable {
  ExprType getExprType();
}
