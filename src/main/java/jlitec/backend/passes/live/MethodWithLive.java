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
    argsWithThis = Collections.unmodifiableList(argsWithThis);
    vars = Collections.unmodifiableList(vars);
    blockWithLiveList = Collections.unmodifiableList(blockWithLiveList);
    lowerStmtWithLiveList = Collections.unmodifiableList(lowerStmtWithLiveList);
  }
}
