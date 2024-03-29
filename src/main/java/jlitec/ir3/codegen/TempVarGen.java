package jlitec.ir3.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jlitec.ir3.Type;
import jlitec.ir3.Var;

public class TempVarGen {
  private int counter = 1;
  private List<Var> vars = new ArrayList<>();
  private final String prefix;

  public TempVarGen() {
    prefix = "_t";
  }

  public TempVarGen(String prefix) {
    this.prefix = prefix;
  }

  public TempVarGen(String prefix, int counter) {
    this.prefix = prefix;
    this.counter = counter;
  }

  public Var gen(Type type) {
    final var result = new Var(type, prefix + counter);
    counter++;
    vars.add(result);
    return result;
  }

  public List<Var> getVars() {
    return Collections.unmodifiableList(vars);
  }
}
