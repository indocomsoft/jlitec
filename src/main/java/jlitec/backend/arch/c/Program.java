package jlitec.backend.arch.c;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.Printable;

public record Program(List<Struct> structs, List<Method> methods) implements Printable {
  static final String[] LIBRARIES = new String[] {"stdio", "stdlib", "string", "stdbool"};

  public Program {
    structs = Collections.unmodifiableList(structs);
    methods = Collections.unmodifiableList(methods);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();

    for (final var lib : LIBRARIES) {
      indent(sb, indent);
      sb.append("#include <" + lib + ".h>").append("\n");
    }

    for (final var struct : structs) {
      sb.append(struct.print(indent)).append("\n");
    }

    // Print function prototypes
    for (final var method : methods) {
      final var name = method.name().equals("main") ? "realmain" : method.name();
      indent(sb, indent);
      sb.append("static ");
      if (name.equals("realmain")) {
        sb.append("inline ");
      }
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

    sb.append(
            """
            static inline char* getline_without_newline() {
              char* result = NULL;
              size_t n = 0;
              ssize_t len = getline(&result, &n, stdin);
              result[len - 1] = 0;
              return realloc(result, len - 1);
            }

            static inline int readln_int() {
              int a;
              char* result = NULL;
              size_t n = 0;
              getline(&result, &n, stdin);
              sscanf(result, "%d", &a);
              free(result);
              return a;
            }

            static inline bool readln_bool() {
              bool a;
              char* result = NULL;
              size_t n = 0;
              getline(&result, &n, stdin);
              a = strncmp(result, "true", 4) == 0;
              free(result);
              return a;
            }
            """)
        .append("\n");

    for (final var method : methods) {
      sb.append(method.print(indent)).append("\n");
    }

    indent(sb, indent);
    sb.append("int main() {\n");
    indent(sb, indent + 1);
    sb.append("realmain();\n");
    indent(sb, indent + 1);
    sb.append("return 0;\n");
    indent(sb, indent);
    sb.append("}\n\n");

    return sb.toString();
  }
}
