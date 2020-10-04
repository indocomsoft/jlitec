package jlitec.ast;

public record PrimitiveType(JliteType jliteType) implements Type {
  public PrimitiveType {
    assert jliteType != JliteType.CLASS;
  }
}
