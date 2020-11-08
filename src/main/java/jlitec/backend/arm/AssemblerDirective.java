package jlitec.backend.arm;

public record AssemblerDirective(String key, String value) implements Insn {
  @Override
  public String print(int indent) {
    return "." + key + " " + value;
  }
}
