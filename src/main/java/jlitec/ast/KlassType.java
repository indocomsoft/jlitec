package jlitec.ast;

public record KlassType(String cname) implements Type {
  @Override
  public JliteType type() {
    return JliteType.CLASS;
  }

  @Override
  public String print(int indent) {
    return this.cname;
  }
}
