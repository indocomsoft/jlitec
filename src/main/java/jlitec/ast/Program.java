package jlitec.ast;

import java.util.Collections;
import java.util.List;

public record Program(List<Klass> klassList) implements Printable {
  public Program {
    this.klassList = Collections.unmodifiableList(klassList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    for (final var klass : klassList) {
      sb.append(klass.print(indent));
      sb.append('\n');
    }
    return sb.toString();
  }
}
