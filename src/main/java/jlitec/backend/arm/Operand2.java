package jlitec.backend.arm;

import jlitec.Printable;

/*
Not implemented:
  - opsh
  - rotate/shift by register
 */
public interface Operand2 extends Printable {
  record Register(jlitec.backend.arm.Register reg) implements Operand2 {
    @Override
    public String print(int indent) {
      return reg.name();
    }
  }

  record Immediate(int value) implements Operand2 {
    public Immediate {
      throwOnInvalid(value);
    }

    @Override
    public String print(int indent) {
      return "#" + value;
    }

    public static boolean isValid(int value) {
      try {
        throwOnInvalid(value);
      } catch (RuntimeException e) {
        return false;
      }
      return true;
    }

    private static void throwOnInvalid(int value) {
      final var leading = Integer.numberOfLeadingZeros(value);
      final var trailing = Integer.numberOfTrailingZeros(value);
      final var length = 32 - leading - trailing;
      // Requirement:
      // any value that can be produced by rotating an 8-bit value right by any even number of bits
      // within a 32-bit word
      if (length > 8) {
        throw new RuntimeException(
            "Operand2 immediate value must be a rotated 8-bit value, instead found `"
                + value
                + "' which is a rotated "
                + length
                + "-bit value.");
      }
      if (length == 8 && (trailing & 1) == 1) {
        throw new RuntimeException(
            "Operand2 immediate value can only be rotated by an even number of bits, instead found `"
                + value
                + "' which requires rotation by "
                + trailing
                + "bits.");
      }
    }
  }
}
