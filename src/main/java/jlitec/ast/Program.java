package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record Program(List<Klass> klassList) {
  public Program {
    this.klassList = Collections.unmodifiableList(klassList);
  }
}
