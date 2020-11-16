package jlitec.backend.passes.regalloc;

import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.Pass;

public interface RegAllocPass<Register>
    extends Pass<MethodWithFlow, MethodWithRegAlloc<Register>> {}
