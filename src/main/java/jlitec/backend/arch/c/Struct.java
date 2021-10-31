package jlitec.backend.arch.c;

import java.util.Collections;
import java.util.List;
import jlitec.Printable;

public record Struct(String name, List<Var> fields) implements Printable {
  public Struct {
    fields = Collections.unmodifiableList(fields);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("struct ").append(name).append(" {\n");

    for (final var field : fields) {
      indent(sb, indent + 1);
      sb.append(field.type().print(indent)).append(" ").append(field.id()).append(";\n");
    }

    indent(sb, indent);
    sb.append("};\n");
    return sb.toString();
  }
}
