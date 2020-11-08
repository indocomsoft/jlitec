package jlitec.backend.arm;

public enum Register {
  R0,
  R1,
  R2,
  R3,
  R4,
  R5,
  R6,
  R7,
  R8,
  R9,
  R10,
  R11,
  R12,
  SP,
  LR,
  PC;

  public static int USER_MAX = 12;

  public static Register fromInt(int i) {
    return switch (i) {
      case 0 -> R0;
      case 1 -> R1;
      case 2 -> R2;
      case 3 -> R3;
      case 4 -> R4;
      case 5 -> R5;
      case 6 -> R6;
      case 7 -> R7;
      case 8 -> R8;
      case 9 -> R9;
      case 10 -> R10;
      case 11 -> R11;
      case 12 -> R12;
      case 13 -> SP;
      case 14 -> LR;
      case 15 -> PC;
      default -> throw new RuntimeException("Invalid register number");
    };
  }
}
