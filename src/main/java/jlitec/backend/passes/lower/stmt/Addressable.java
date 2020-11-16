package jlitec.backend.passes.lower.stmt;

import jlitec.Printable;
import jlitec.backend.arch.arm.Register;
import jlitec.ir3.expr.rval.RvalExpr;

public interface Addressable extends Printable {
  record Reg(Register reg) implements Addressable {
    @Override
    public String print(int indent) {
      return reg.name();
    }
  }

  record Rval(RvalExpr rvalExpr) implements Addressable {
    @Override
    public String print(int indent) {
      return rvalExpr.print(indent);
    }
  }
}
