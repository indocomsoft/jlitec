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
      sb.append(", ")
          .append(
              args.stream()
                  .map(v -> v.type().print(0) + " " + v.id())
                  .collect(Collectors.joining(", ")));
    }
    sb.append(") {\n");
    // TODO print body
    indent(sb, indent);
    sb.append("}");
    return sb.toString();
  }
}
