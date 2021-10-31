package jlitec.ir3;

import java.util.Collections;
import java.util.List;
import jlitec.Printable;

public record Program(List<Data> dataList, List<Method> methodList) implements Printable {
  public Program {
    dataList = Collections.unmodifiableList(dataList);
    methodList = Collections.unmodifiableList(methodList);
  }

  @Override
  public String print(int indent) {
    final var sb = new StringBuilder();
    sb.append("======= CData3 =======\n");
    for (final var data : dataList) {
      sb.append(data.print(indent)).append("\n\n");
    }
    sb.append("======= CMtd3 =======\n");
    for (final var method : methodList) {
      sb.append(method.print(indent)).append("\n\n");
    }
    return sb.toString();
  }
}
