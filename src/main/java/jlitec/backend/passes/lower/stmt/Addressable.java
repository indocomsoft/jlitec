package jlitec.backend.passes.lower.stmt;

import jlitec.Printable;
import jlitec.backend.arch.arm.Register;
import jlitec.backend.passes.Node;
import jlitec.ir3.expr.rval.IdRvalExpr;

public interface Addressable extends Printable {
  enum Type {
    REG,
    ID_RVAL;
  }

  Type type();

  Node toNode();

  record Reg(Register reg) implements Addressable {
    @Override
    public String print(int indent) {
      return reg.name();
    }

    @Override
    public Type type() {
      return Type.REG;
    }

    @Override
    public Node toNode() {
      return new Node.Reg(reg);
    }
  }

  record IdRval(IdRvalExpr idRvalExpr) implements Addressable {
    @Override
    public String print(int indent) {
      return idRvalExpr.print(indent);
    }

    @Override
    public Type type() {
      return Type.ID_RVAL;
    }

    @Override
    public Node toNode() {
      return new Node.Id(idRvalExpr);
    }
  }
}
