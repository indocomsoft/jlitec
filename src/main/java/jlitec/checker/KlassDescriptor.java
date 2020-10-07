package jlitec.checker;

import com.google.common.collect.Multimap;
import java.util.Map;

public record KlassDescriptor(
    Map<String, Type.Basic> fields, Map<String, Multimap<Integer, Type.Method>> methods) {
  /**
   * @param fields field descriptors.
   * @param methods method descriptors, mapping name -> arity -> methods.
   */
  public KlassDescriptor {}
}
