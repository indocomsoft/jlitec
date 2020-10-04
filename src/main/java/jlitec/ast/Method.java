package jlitec.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java_cup.runtime.ComplexSymbolFactory.Location;
import jlitec.ast.stmt.Stmt;

public record Method(
    Type type,
    Name name,
    List<Var> args,
    List<Var> vars,
    List<Stmt> stmtList,
    Location leftLocation,
    Location rightLocation)
    implements Printable, Locatable {
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
    sb.append(type.print(indent)).append(' ').append(name.id()).append('(');
    sb.append(
        args.stream()
            .map(arg -> arg.type().print(indent) + ' ' + arg.name().id())
            .collect(Collectors.joining(", ")));
    sb.append(") {\n");

    vars.forEach(
        variable -> {
          indent(sb, indent + 1);
          sb.append(variable.type().print(indent)).append(' ').append(variable.name().id()).append(";\n");
        });

    stmtList.forEach(stmt -> sb.append(stmt.print(indent + 1)));

    indent(sb, indent);
    sb.append("}\n");
    return sb.toString();
  }
}
