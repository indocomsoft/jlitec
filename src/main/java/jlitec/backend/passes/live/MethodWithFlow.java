package jlitec.backend.passes.live;

import jlitec.backend.passes.flow.FlowGraph;

public record MethodWithFlow(jlitec.ir3.Method method, FlowGraph flowGraph) {}
