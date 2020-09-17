package jlitec.ast;

public record PrimitiveType(JliteType type) implements Type {
  public PrimitiveType {
    assert type != JliteType.CLASS;
  }

  @Override
  public String print(int indent) {
    return type.print(indent);
  }
}
