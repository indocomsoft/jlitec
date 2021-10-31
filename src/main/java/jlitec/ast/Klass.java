package jlitec.ast;

import java.util.Collections;
import java.util.List;

public record Klass(String cname, List<Var> fields, List<Method> methods) {
  public Klass {
    fields = Collections.unmodifiableList(fields);
    methods = Collections.unmodifiableList(methods);
  }
}
