package jlitec.backend.passes.lower;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.Printable;
import jlitec.backend.passes.lower.stmt.LowerStmt;
import jlitec.ir3.Type;
import jlitec.ir3.Var;

public record Method(
    Type returnType,
    String id,
    List<Var> argsWithThis,
    List<Var> vars,
    List<Var> spilled,
    List<LowerStmt> lowerStmtList)
    implements Printable {
  public Method {
    this.argsWithThis = Collections.unmodifiableList(argsWithThis);
    this.vars = Collections.unmodifiableList(vars);
    this.spilled = Collections.unmodifiableList(spilled);
    this.lowerStmtList = Collections.unmodifiableList(lowerStmtList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    indent(sb, indent);
    sb.append(returnType.print(0)).append(" ").append(id).append("(");
    sb.append(
        argsWithThis.stream()
            .map(v -> v.type().print(0) + " " + v.id())
            .collect(Collectors.joining(", ")));
    sb.append(") {\n");

    indent(sb, indent + 1);
    sb.append("// START OF SPILLED TO STACK\n");
    for (final var variable : spilled) {
      indent(sb, indent + 1);
      sb.append(variable.type().print(0)).append(' ').append(variable.id()).append(";\n");
    }
    indent(sb, indent + 1);
    sb.append("// END OF SPILLED TO STACK\n");

    for (final var variable : vars) {
      indent(sb, indent + 1);
      sb.append(variable.type().print(0)).append(' ').append(variable.id()).append(";\n");
    }

    for (final var stmt : lowerStmtList) {
      sb.append(stmt.print(indent + 1));
    }

    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
