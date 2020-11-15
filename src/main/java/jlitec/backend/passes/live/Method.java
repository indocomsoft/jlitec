package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.List;
import jlitec.ir3.Type;
import jlitec.ir3.Var;

public record Method(
    Type returnType,
    String id,
    List<Var> argsWithThis,
    List<Var> vars,
    List<BlockWithLive> blockWithLiveList,
    List<StmtWithLive> stmtWithLiveList) {
  public Method {
    this.argsWithThis = Collections.unmodifiableList(argsWithThis);
    this.vars = Collections.unmodifiableList(vars);
    this.blockWithLiveList = Collections.unmodifiableList(blockWithLiveList);
    this.stmtWithLiveList = Collections.unmodifiableList(stmtWithLiveList);
  }
}
