package jlitec.backend.passes.regalloc;

import java.util.Map;

public record MethodWithRegAlloc<Register>(Map<String, Register> regAlloc) {}
