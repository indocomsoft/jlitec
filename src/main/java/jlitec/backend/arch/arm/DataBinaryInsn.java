package jlitec.backend.arch.arm;

public interface DataBinaryInsn extends ARMInsn {
  boolean updateConditionFlags();

  Register dst();

  Register src();

  Operand2 op2();

  Type type();

  enum Type {
    ADD,
    AND,
    EOR,
    ORR,
    RSB,
    SUB;
  }

  @Override
  default String print(int indent) {
    return type().name()
        + condition().print(0)
        + (updateConditionFlags() ? "S" : "")
        + " "
        + dst().name()
        + ", "
        + src().name()
        + ", "
        + op2().print(0);
  }
}
