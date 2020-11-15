package jlitec.backend.passes.flow;

import java.util.Collections;
import java.util.Map;
import jlitec.ir3.Method;
import jlitec.ir3.Program;

public record ProgramWithFlow(Program program, Map<Method, FlowGraph> methodToFlow) {
  public ProgramWithFlow {
    this.methodToFlow = Collections.unmodifiableMap(methodToFlow);
  }
}
