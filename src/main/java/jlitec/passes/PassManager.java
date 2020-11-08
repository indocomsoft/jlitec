package jlitec.passes;

import jlitec.ir3.Method;
import jlitec.ir3.Program;
import jlitec.passes.flow.FlowGraph;
import jlitec.passes.flow.FlowPass;

import java.util.Map;

public class PassManager {
  // Prevent instantiation
  private PassManager() {}

  public static Map<Method, FlowGraph> run(Program input, boolean optimization) {
    final var flowOutput = new FlowPass().pass(input);
    return flowOutput;
  }
}
