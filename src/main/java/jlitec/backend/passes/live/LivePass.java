package jlitec.backend.passes.live;

import jlitec.ir3.Var;
import jlitec.backend.passes.Pass;
import jlitec.ir3.stmt.Stmt;

import java.util.Collections;
import java.util.Set;

public class LivePass implements Pass<jlitec.ir3.Method, Method> {
  private record StmtDefUse(Stmt stmt, Set<Var> use, Set<Var> def) {
    public StmtDefUse {
      this.use = Collections.unmodifiableSet(use);
      this.def = Collections.unmodifiableSet(def);
    }
  }

  @Override
  public Method pass(jlitec.ir3.Method input) {
    return null;
  }
}
