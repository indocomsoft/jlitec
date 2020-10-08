package jlitec.ir3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Ir3CodeGen {
  // prevent instantiation
  private Ir3CodeGen() {}

  public static Program generate(jlitec.ast.Program program) {
    final var dataList =
        program.klassList().stream()
            .map(
                k ->
                    new Data(
                        k.cname(),
                        k.fields().stream().map(Var::new).collect(Collectors.toUnmodifiableList())))
            .collect(Collectors.toUnmodifiableList());
    // TODO generate code for program
    final var methodList = new ArrayList<Method>();
    for (final var klass : program.klassList()) {
      int counter = 0;
      for (final var method : klass.methods()) {
        methodList.add(
            new Method(
                klass.cname(),
                Type.fromAst(method.returnType()),
                method.id().equals("main") ? "main" : "%" + klass.cname() + counter,
                method.args().stream().map(Var::new).collect(Collectors.toUnmodifiableList()),
                method.vars().stream().map(Var::new).collect(Collectors.toUnmodifiableList()),
                // TODO generate list of instructions
                List.of()));
        counter++;
      }
    }

    return new Program(dataList, methodList);
  }
}
