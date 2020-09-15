package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.ast.stmt.Stmt;

public record Method(Type type, String id, List<Var> args, List<Var> vars, List<Stmt> stmtList)
    implements Printable {
  /** Construct a Method in the AST. */
  public Method {
    this.args = Collections.unmodifiableList(args);
    this.vars = Collections.unmodifiableList(vars);
    this.stmtList = Collections.unmodifiableList(stmtList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(type.toString()).append(' ').append(id).append('(');
    final var argListString =
        args.stream()
            .map(
                arg ->
                    new StringBuilder()
                        .append(arg.type().toString())
                        .append(' ')
                        .append(arg.id())
                        .toString())
            .collect(Collectors.joining(", "));
    ;
    sb.append(argListString);
    sb.append(") {\n");

    for (final var variable : vars) {
      indent(sb, indent + 1);
      sb.append(variable.type().toString()).append(' ').append(variable.id()).append(";\n");
    }

    for (final var stmt : stmtList) {
      sb.append(stmt.print(indent + 1));
    }

    indent(sb, indent);
    sb.append("}\n");
    return sb.toString();
  }
}
