package jlitec.backend.passes.live;

import java.util.Collections;
import java.util.List;
import jlitec.ir3.Type;
import jlitec.ir3.Var;

public record MethodWithLive(
    Type returnType,
    String id,
    List<Var> argsWithThis,
    List<Var> vars,
    List<BlockWithLive> blockWithLiveList,
    List<LowerStmtWithLive> lowerStmtWithLiveList) {
  public MethodWithLive {
    this.argsWithThis = Collections.unmodifiableList(argsWithThis);
    this.vars = Collections.unmodifiableList(vars);
    this.blockWithLiveList = Collections.unmodifiableList(blockWithLiveList);
    this.lowerStmtWithLiveList = Collections.unmodifiableList(lowerStmtWithLiveList);
  }
}
