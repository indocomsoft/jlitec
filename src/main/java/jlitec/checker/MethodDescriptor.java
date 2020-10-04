package jlitec.checker;

import java.util.List;
import jlitec.parsetree.Type;

public record MethodDescriptor(List<Type> argTypes, Type returnType) {}
