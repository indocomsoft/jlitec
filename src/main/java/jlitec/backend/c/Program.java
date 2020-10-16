package jlitec.backend.c;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.Printable;

public record Program(List<Struct> structs, List<Method> methods) implements Printable {
  public Program {
    this.structs = Collections.unmodifiableList(structs);
    this.methods = Collections.unmodifiableList(methods);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    sb.append(
            """
            #include <stdio.h>
            #include <stdlib.h>
            #include <string.h>

            char* getline_without_newline() {
              char* result = NULL;
              size_t n = 0;
              ssize_t len;
              len = getline(&result, &n, stdin);
              result[len - 1] = 0;
              return realloc(result, len - 1);
            }
            """)
        .append("\n");

    for (final var struct : structs) {
      sb.append(struct.print(indent)).append("\n");
    }

    // Print function prototypes
    for (final var method : methods) {
      final var name = method.name().equals("main") ? "realmain" : method.name();
      indent(sb, indent);
      sb.append(method.returnType().print(indent)).append(" ").append(name).append("(");
      if (!name.equals("realmain")) {
        sb.append(
            method.args().stream()
                .map(a -> a.type().print(indent) + " " + a.id())
                .collect(Collectors.joining(", ")));
      }
      sb.append(");\n");
    }
    sb.append("\n");

    indent(sb, indent);
    sb.append("int main() {\n");
    indent(sb, indent + 1);
    sb.append("realmain();\n");
    indent(sb, indent + 1);
    sb.append("return 0;\n");
    indent(sb, indent);
    sb.append("}\n\n");

    for (final var method : methods) {
      sb.append(method.print(indent)).append("\n");
    }
    return sb.toString();
  }
}
