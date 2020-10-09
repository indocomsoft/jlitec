package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record Klass(String cname, List<Var> fields, List<Method> methods) {
  public Klass {
    this.fields = Collections.unmodifiableList(fields);
    this.methods = Collections.unmodifiableList(methods);
  }
}
