package jlitec.backend.arch.c;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.Printable;
import jlitec.backend.arch.c.stmt.LabelStmt;
import jlitec.backend.arch.c.stmt.Stmt;

public record Method(
    String name, Type returnType, List<Var> args, List<Var> vars, List<Stmt> stmtList)
    implements Printable {
  public Method {
    args = Collections.unmodifiableList(args);
    vars = Collections.unmodifiableList(vars);
    stmtList = Collections.unmodifiableList(stmtList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();

    final var methodName = name.equals("main") ? "realmain" : name;

    indent(sb, indent);
    sb.append("static ");
    if (methodName.equals("realmain")) {
      sb.append("inline ");
    }
    sb.append(returnType.print(indent)).append(" ").append(methodName).append("(");
    if (!methodName.equals("realmain")) {
      sb.append(
          args.stream()
              .map(arg -> arg.type().print(indent) + " " + arg.id())
              .collect(Collectors.joining(", ")));
    }
    sb.append(") {\n");

    for (final var variable : vars) {
      indent(sb, indent + 1);
      sb.append(variable.type().print(indent)).append(" ").append(variable.id()).append(";\n");
    }

    for (final var stmt : stmtList) {
      sb.append(stmt.print(indent + 1));
    }

    final var finalStmt = stmtList.get(stmtList.size() - 1);
    if (finalStmt instanceof LabelStmt) {
      indent(sb, indent + 1);
      sb.append(";\n");
    }

    indent(sb, indent);
    sb.append("}\n");
    return sb.toString();
  }
}
