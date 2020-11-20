package jlitec.backend.arch.arm.codegen;

public interface LocationDescriptor {
  record Stack(int offset) implements LocationDescriptor {}
}
