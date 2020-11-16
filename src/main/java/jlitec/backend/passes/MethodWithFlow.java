package jlitec.backend.passes;

import jlitec.backend.passes.flow.FlowGraph;

public record MethodWithFlow(jlitec.ir3.Method method, FlowGraph flowGraph) {}
