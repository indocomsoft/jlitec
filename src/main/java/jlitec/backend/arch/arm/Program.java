package jlitec.backend.arch.arm;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jlitec.Printable;

public record Program(List<Insn> insnList) implements Printable {
  public Program {
    insnList = Collections.unmodifiableList(insnList);
  }

  @Override
  public String print(int indent) {
    return insnList.stream().map(i -> i.print(indent)).collect(Collectors.joining("\n"));
  }
}
