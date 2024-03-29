package jlitec.backend.arch.c.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jlitec.backend.arch.c.Type;
import jlitec.backend.arch.c.Var;

public class TempVarGen {
  private int counter = 0;
  private List<Var> vars = new ArrayList<>();

  public List<Var> getAllVars() {
    return Collections.unmodifiableList(vars);
  }

  public Var gen(Type type) {
    final var newVar = new Var(type, "__t" + counter);
    vars.add(newVar);
    counter++;
    return newVar;
  }
}
