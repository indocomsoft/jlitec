package jlitec.ast;

public record KlassType(String cname) implements Type {
  @Override
  public JliteType jliteType() {
    return JliteType.CLASS;
  }
}
