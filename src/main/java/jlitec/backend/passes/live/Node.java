package jlitec.backend.passes.live;

import jlitec.Printable;
import jlitec.backend.arch.arm.Register;
import jlitec.ir3.expr.rval.IdRvalExpr;

public interface Node extends Printable, Comparable<Node> {
  enum Type {
    REG,
    ID;
  }

  Type type();

  @Override
  default int compareTo(Node o) {
    return switch (this.type()) {
      case ID -> switch (o.type()) {
        case ID -> ((Id) this).id.compareTo(((Id) o).id);
        case REG -> -1;
      };
      case REG -> switch (o.type()) {
        case ID -> 1;
        case REG -> ((Reg) this).reg.compareTo(((Reg) o).reg);
      };
    };
  }

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
