package jlitec.checker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Environment {
  public static record MethodDescriptor(String name, List<String> argTypes) {
    public MethodDescriptor {
      this.argTypes = Collections.unmodifiableList(argTypes);
    }
  }

  private final ListMultimap<String, Type.Basic> map;
  private final Map<MethodDescriptor, Type.Basic> methodDescriptors;

  public Environment() {
    this.map = ImmutableListMultimap.of();
    this.methodDescriptors = Map.of();
  }

  private Environment(
      Multimap<String, Type.Basic> map, Map<MethodDescriptor, Type.Basic> methodDescriptors) {
    this.map = ImmutableListMultimap.copyOf(map);
    this.methodDescriptors = Collections.unmodifiableMap(methodDescriptors);
  }

  public Environment(KlassDescriptor klassDescriptor) {
    final ArrayListMultimap<String, Type.Basic> map =
        klassDescriptor.fields().entrySet().stream()
            .collect(
                ArrayListMultimap::create,
                (m, e) -> m.put(e.getKey(), e.getValue()),
                ArrayListMultimap::putAll);
    this.map = Multimaps.unmodifiableListMultimap(map);
    this.methodDescriptors =
        klassDescriptor.methods().entrySet().stream()
            .flatMap(
                e ->
                    e.getValue().stream()
                        .map(
                            m ->
                                new AbstractMap.SimpleImmutableEntry<>(
                                    new MethodDescriptor(
                                        e.getKey(),
                                        m.argTypes().stream()
                                            .map(Type.Basic::type)
                                            .collect(Collectors.toUnmodifiableList())),
                                    m.returnType())))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Optional<Type.Basic> lookup(String id) {
    return Optional.ofNullable(Iterables.getLast(map.get(id), null));
  }

  public Optional<Type.Basic> lookup(MethodDescriptor methodDescriptor) {
    return Optional.ofNullable(methodDescriptors.getOrDefault(methodDescriptor, null));
  }

  public Environment augment(Environment env) {
    final var map = ArrayListMultimap.create(this.map);
    final var methodDescriptors = new HashMap<>(this.methodDescriptors);
    map.putAll(env.map);
    methodDescriptors.putAll(env.methodDescriptors);
    return new Environment(map, methodDescriptors);
  }

  public Environment augment(String id, Type.Basic type) {
    final var temp = ArrayListMultimap.create(map);
    temp.put(id, type);
    return new Environment(temp, methodDescriptors);
  }
}
