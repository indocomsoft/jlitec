package jlitec.checker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Optional;

public class Environment {
  private final ListMultimap<String, Type> map;

  public Environment() {
    this.map = ImmutableListMultimap.of();
  }

  private Environment(Multimap<String, Type> map) {
    this.map = ImmutableListMultimap.copyOf(map);
  }

  public Environment(KlassDescriptor klassDescriptor) {
    final ArrayListMultimap<String, Type> map =
        klassDescriptor.fields().entrySet().stream()
            .collect(
                ArrayListMultimap::create,
                (m, e) -> m.put(e.getKey(), e.getValue()),
                ArrayListMultimap::putAll);
    this.map = Multimaps.unmodifiableListMultimap(map);
  }

  public Optional<Type> lookup(String id) {
    return Optional.ofNullable(Iterables.getLast(map.get(id), null));
  }

  public Environment augment(Environment env) {
    final var map = ArrayListMultimap.create(this.map);
    map.putAll(env.map);
    return new Environment(map);
  }

  public Environment augment(String id, Type type) {
    final var temp = ArrayListMultimap.create(map);
    temp.put(id, type);
    return new Environment(temp);
  }
}
