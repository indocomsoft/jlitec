package jlitec.ir3;

import java.util.Collections;
import java.util.List;
import jlitec.Printable;

public record Data(String cname, List<Var> fields) implements Printable {
  public Data {
    this.fields = Collections.unmodifiableList(fields);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append("class ").append(cname).append(" {\n");
    for (final var field : fields) {
      indent(sb, indent + 1);
      sb.append(field.type().print(0)).append(' ').append(field.id()).append(";\n");
    }
    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
