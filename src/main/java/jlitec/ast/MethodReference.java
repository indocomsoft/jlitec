package jlitec.ast;

import java.util.List;

public record MethodReference(
    String cname, String methodName, Type returnType, List<Type> argTypes) {}
