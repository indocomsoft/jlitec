package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record Program(List<Klass> klassList, Location leftLocation, Location rightLocation)
    implements Printable, Locatable {
  public Program {
    this.klassList = Collections.unmodifiableList(klassList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    klassList.forEach(klass -> sb.append(klass.print(indent)).append('\n'));
    return sb.toString();
  }
}
