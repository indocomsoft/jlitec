package jlitec.checker;

import java.util.List;
import java.util.Map;

public record KlassDescriptor(
    Map<String, Type> fields, Map<String, List<MethodDescriptor>> methods) {
  public static record MethodDescriptor(Type returnType, List<Type> argTypes) {}
}
