package jlitec.ir3;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.Printable;
import jlitec.ir3.stmt.Stmt;

public record Method(
    String cname, Type returnType, String id, List<Var> args, List<Var> vars, List<Stmt> stmtList)
    implements Printable {
  public Method {
    this.args = Collections.unmodifiableList(args);
    this.vars = Collections.unmodifiableList(vars);
    this.stmtList = Collections.unmodifiableList(stmtList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(returnType.print(0)).append(" ").append(id).append("(").append(cname).append(" this");
    if (!args.isEmpty()) {
      sb.append(", ");
      sb.append(
          args.stream()
              .map(v -> v.type().print(0) + " " + v.id())
              .collect(Collectors.joining(", ")));
    }
    sb.append(") {\n");

    for (final var variable : vars) {
      indent(sb, indent + 1);
      sb.append(variable.type().print(0)).append(' ').append(variable.id()).append(";\n");
    }

    // TODO print stmtList

    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
