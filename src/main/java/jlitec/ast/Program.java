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
    klassList.forEach(klass -> sb.append(klass.print(indent)).append('\n'));
    return sb.toString();
  }
}
