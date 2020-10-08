package jlitec.ir3;

import java.util.Collections;
import java.util.List;

public record Data(String cname, List<Var> vars) {
  public Data {
    this.vars = Collections.unmodifiableList(vars);
  }
}
