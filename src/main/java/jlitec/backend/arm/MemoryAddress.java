package jlitec.backend.arm;

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
    @Override
    public String print(int indent) {
      final var offset = maybeOffset.map(o -> ", #" + o).orElse("");
      return "[" + register.name() + offset + "]" + (writeback ? "!" : "");
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
