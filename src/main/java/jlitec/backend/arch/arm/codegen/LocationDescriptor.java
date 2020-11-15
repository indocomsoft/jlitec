package jlitec.backend.arch.arm.codegen;

import jlitec.backend.arch.arm.Register;

public interface LocationDescriptor {
  Location location();

  enum Location {
    REGISTER,
    STACK;
  }

  record Reg(Register register) implements LocationDescriptor {
    @Override
    public Location location() {
      return Location.REGISTER;
    }
  }

  record Stack(int offset) implements LocationDescriptor {
    @Override
    public Location location() {
      return Location.STACK;
    }
  }
}
