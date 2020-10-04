package jlitec.parsetree;

import java_cup.runtime.ComplexSymbolFactory.Location;

public record PrimitiveType(JliteType type, Location leftLocation, Location rightLocation)
    implements Type {
  public PrimitiveType {
    assert type != JliteType.CLASS;
  }

  @Override
  public String print(int indent) {
    return type.print(indent);
  }
}
