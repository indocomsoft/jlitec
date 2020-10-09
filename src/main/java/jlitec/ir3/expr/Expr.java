package jlitec.ir3.expr;

import jlitec.Printable;

public interface Expr extends Printable {
  ExprType getExprType();
}
