package jlitec.backend.passes;

import jlitec.backend.passes.flow.FlowGraph;

public record MethodWithFlow(jlitec.backend.passes.lower.Method method, FlowGraph flowGraph) {}
