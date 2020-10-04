package jlitec.checker;

import java.util.List;
import jlitec.ast.Type;

public record MethodDescriptor(List<Type> argTypes, Type returnType) {}
