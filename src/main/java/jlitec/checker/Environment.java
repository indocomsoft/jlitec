package jlitec.checker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.Optional;

public class Environment {
  public final ImmutableListMultimap<String, Type> map;

  public Environment() {
    this.map = ImmutableListMultimap.of();
  }

  private Environment(Multimap<String, Type> map) {
    this.map = ImmutableListMultimap.copyOf(map);
  }

  public Environment(KlassDescriptor klassDescriptor) {
    final ArrayListMultimap<String, Type> temp = ArrayListMultimap.create();
    for (final var entry: klassDescriptor.fields().entrySet()) {
      temp.put(entry.getKey(), entry.getValue());
    }
    for (final var entry: klassDescriptor.methods().entrySet()) {
      final var methodName = entry.getKey();
      for (final var aryEntry: entry.getValue().values()) {
        temp.put(methodName, aryEntry.returnType());
      }
    }
    this.map = ImmutableListMultimap.copyOf(temp);
  }

  public Optional<Type> lookup(String id) {
    return Optional.ofNullable(Iterables.getLast(map.get(id), null));
  }

  public Environment augment(Environment env) {
    final var temp = ArrayListMultimap.create(map);
    temp.putAll(env.map);
    return new Environment(temp);
  }
  public Environment augment(String id, Type type) {
    final var temp = ArrayListMultimap.create(map);
    temp.put(id, type);
    return new Environment(temp);
  }
}
