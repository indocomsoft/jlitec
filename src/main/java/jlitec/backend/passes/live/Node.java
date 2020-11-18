package jlitec.backend.passes.live;

import jlitec.Printable;
import jlitec.backend.arch.arm.Register;
import jlitec.ir3.expr.rval.IdRvalExpr;

public interface Node extends Printable {
  enum Type {
    REG,
    ID;
  }

  Type type();

  record Reg(Register reg) implements Node {
    @Override
    public Type type() {
      return Type.REG;
    }

    @Override
    public String print(int indent) {
      return "_Reg_" + reg.name();
    }
  }

  record Id(String id) implements Node {
    public Id(IdRvalExpr idRvalExpr) {
      this(idRvalExpr.id());
    }

    @Override
    public Type type() {
      return Type.ID;
    }

    @Override
    public String print(int indent) {
      return id;
    }
  }
}
