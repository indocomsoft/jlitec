package jlitec.backend.passes.lower.stmt;

import jlitec.Printable;
import jlitec.backend.arch.arm.Register;
import jlitec.ir3.expr.rval.RvalExpr;

public interface Addressable extends Printable {
  enum Type {
    REG,
    RVAL;
  }

  Type type();

  record Reg(Register reg) implements Addressable {
    @Override
    public String print(int indent) {
      return reg.name();
    }

    @Override
    public Type type() {
      return Type.REG;
    }
  }

  record Rval(RvalExpr rvalExpr) implements Addressable {
    @Override
    public String print(int indent) {
      return rvalExpr.print(indent);
    }

    @Override
    public Type type() {
      return Type.RVAL;
    }
  }
}
