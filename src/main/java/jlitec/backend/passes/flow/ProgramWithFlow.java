package jlitec.backend.passes.flow;

import jlitec.ir3.Method;
import jlitec.ir3.Program;

import java.util.Collections;
import java.util.Map;

public record ProgramWithFlow(Program program, Map<Method, FlowGraph> methodToFlow) {
  public ProgramWithFlow {
    this.methodToFlow = Collections.unmodifiableMap(methodToFlow);
  }
}
