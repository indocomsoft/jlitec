package jlitec.checker;

import java.util.List;
import java.util.stream.Collectors;

public interface Type {
  record Basic(jlitec.parsetree.Type type) implements Type {}
  record Method(List<Basic> args, Basic returnType) implements Type {
    public Method(jlitec.parsetree.Method method) {
      this(method.args().stream().map(m -> new Basic(m.type())).collect(Collectors.toUnmodifiableList()), new Basic(method.type()));
    }
  }
}
