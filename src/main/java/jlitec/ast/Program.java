package jlitec.ast;

import java.util.Collections;
import java.util.List;

public record Program(List<Klass> klassList) {
  public Program {
    this.klassList = Collections.unmodifiableList(klassList);
  }
}
