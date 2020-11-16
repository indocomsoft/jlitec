package jlitec.backend.passes.flow;

import java.util.Collections;
import java.util.Map;
import jlitec.backend.passes.lower.Method;
import jlitec.backend.passes.lower.Program;

public record ProgramWithFlow(Program program, Map<Method, FlowGraph> methodToFlow) {
  public ProgramWithFlow {
    this.methodToFlow = Collections.unmodifiableMap(methodToFlow);
  }
}
