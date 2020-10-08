package jlitec.parsetree;

import java.util.Collections;
import java.util.List;
import java_cup.runtime.ComplexSymbolFactory.Location;
import jlitec.Printable;

public record Klass(
    Name name,
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
    sb.append("class ").append(name.id()).append(" {\n");
    fields.forEach(
        field -> {
          indent(sb, indent + 1);
          sb.append(field.type().print(indent)).append(' ').append(field.name().id()).append(";\n");
        });
    methods.forEach(method -> sb.append(method.print(indent + 1)));
    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
