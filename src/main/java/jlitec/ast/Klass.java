package jlitec.ast;

import java.util.Collections;
import java.util.List;

public record Klass(String cname, List<Var> fields, List<Method> methods) implements Printable {
  public Klass {
    this.fields = Collections.unmodifiableList(fields);
    this.methods = Collections.unmodifiableList(methods);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("class ").append(cname).append(" {\n");
    for (final var field : fields) {
      indent(sb, indent + 1);
      sb.append(field.type().print(indent)).append(" ").append(field.id()).append(";\n");
    }
    for (final var method : methods) {
      sb.append(method.print(indent + 1));
    }
    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
