package jlitec.backend.passes.lower;

import java.util.Collections;
import java.util.List;
import jlitec.Printable;
import jlitec.ir3.Data;

public record Program(List<Data> dataList, List<Method> methodList) implements Printable {
  public Program {
    this.dataList = Collections.unmodifiableList(dataList);
    this.methodList = Collections.unmodifiableList(methodList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    sb.append("======= Data =======\n");
    for (final var data : dataList) {
      sb.append(data.print(indent)).append("\n\n");
    }
    sb.append("======= Methods =======\n");
    for (final var method : methodList) {
      sb.append(method.print(indent)).append("\n\n");
    }
    return sb.toString();
  }
}
