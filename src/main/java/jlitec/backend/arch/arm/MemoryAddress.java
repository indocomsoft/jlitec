package jlitec.backend.arch.arm;

import java.util.Optional;
import jlitec.Printable;

public interface MemoryAddress extends Printable {
  /*
  Not inside (because not in arm_structs.ml):
    - Register offset operation (PLUS/MINUS, assumed PLUS; <opsh>)
    - Post-indexed, register
    - Register offset
   */
  record ImmediateOffset(Register register, Optional<Integer> maybeOffset, boolean writeback)
      implements MemoryAddress {
    public ImmediateOffset(Register register) {
      this(register, Optional.empty(), false);
    }

    public ImmediateOffset(Register register, int offset) {
      this(register, Optional.of(offset), false);
    }

    public ImmediateOffset(Register register, int offset, boolean writeback) {
      this(register, Optional.of(offset), writeback);
    }

    @Override
    public String print(int indent) {
      final var offset = maybeOffset.filter(o -> o != 0).map(o -> ", #" + o).orElse("");
      return "[" + register.name() + offset + "]" + (writeback ? "!" : "");
    }
  }

  record RegisterOffset(Register base, Register offset, boolean writeback)
      implements MemoryAddress {
    public RegisterOffset(Register base, Register offset) {
      this(base, offset, false);
    }

    @Override
    public String print(int indent) {
      return "[" + base.name() + ", " + offset.name() + "]" + (writeback ? "!" : "");
    }
  }

  record PostIndexedImmediate(Register register, int offset) implements MemoryAddress {
    @Override
    public String print(int indent) {
      return "[" + register.name() + "], #" + offset;
    }
  }

  record PCRelative(String label) implements MemoryAddress {
    @Override
    public String print(int indent) {
      return label;
    }
  }
}
