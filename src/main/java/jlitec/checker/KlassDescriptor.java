package jlitec.checker;

import java.util.List;
import java.util.Map;

public record KlassDescriptor(
    Map<String, Type.Basic> fields, Map<String, List<MethodDescriptor>> methods) {
  public static record MethodDescriptor(Type.Basic returnType, List<Type.Basic> argTypes) {}
}
