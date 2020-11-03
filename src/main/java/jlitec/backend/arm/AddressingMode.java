package jlitec.backend.arm;

import java.util.Optional;

public interface AddressingMode {
  /*
  Not inside (because not in arm_structs.ml):
    - Register offset operation (PLUS/MINUS, assumed PLUS; <opsh>)
    - Post-indexed, register
    - Register offset
   */
  record ImmediateOffset(Register register, Optional<Integer> offset, boolean writeback)
      implements AddressingMode {}

  record PostIndexedImmediate(Register register, int offset) implements AddressingMode {}

  record PCRelative(String label) implements AddressingMode {}
}
