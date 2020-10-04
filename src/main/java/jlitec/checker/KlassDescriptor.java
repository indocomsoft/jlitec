package jlitec.checker;

import com.google.common.collect.Multimap;
import java.util.Map;
import jlitec.ast.Type;

public record KlassDescriptor(
    Map<String, Type> fields, Map<String, Multimap<Integer, MethodDescriptor>> methods) {
  /**
   * @param fields field descriptors.
   * @param methods method descriptors, mapping name -> arity -> methods.
   */
  public KlassDescriptor {}
}
