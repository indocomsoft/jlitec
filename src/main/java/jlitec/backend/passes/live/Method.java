package jlitec.backend.passes.live;

import jlitec.ast.Var;
import jlitec.ir3.Type;

import java.util.Collections;
import java.util.List;

public record Method(Type returnType, String id, List<Var> argsWithThis, List<Var> vars, List<StmtWithLiveInfo> stmtWithLifeInfoList) {
  public Method {
    this.argsWithThis = Collections.unmodifiableList(argsWithThis);
    this.vars = Collections.unmodifiableList(vars);
    this.stmtWithLifeInfoList = Collections.unmodifiableList(stmtWithLifeInfoList);
  }
}
