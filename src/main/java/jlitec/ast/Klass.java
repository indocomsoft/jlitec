package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java_cup.runtime.ComplexSymbolFactory.Location;

public record Klass(
    String cname,
    List<Var> fields,
    List<Method> methods,
    Location leftLocation,
    Location rightLocation)
    implements Printable, Locatable {
  public Klass {
    this.fields = Collections.unmodifiableList(fields);
    this.methods = Collections.unmodifiableList(methods);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("class ").append(cname).append(" {\n");
    fields.forEach(
        field -> {
          indent(sb, indent + 1);
          sb.append(field.type().print(indent)).append(' ').append(field.id()).append(";\n");
        });
    methods.forEach(method -> sb.append(method.print(indent + 1)));
    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
