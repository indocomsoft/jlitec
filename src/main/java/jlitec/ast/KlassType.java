package jlitec.ast;

import java_cup.runtime.ComplexSymbolFactory.Location;

public record KlassType(String cname, Location leftLocation, Location rightLocation)
    implements Type {
  @Override
  public JliteType type() {
    return JliteType.CLASS;
  }

  @Override
  public String print(int indent) {
    return this.cname;
  }
}
