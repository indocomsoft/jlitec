package jlitec.checker;

import com.google.common.collect.Multimaps;
import java.util.Map;
import java.util.stream.Collectors;
import jlitec.ast.Klass;
import jlitec.ast.Method;
import jlitec.ast.Program;
import jlitec.ast.Var;

public class StaticChecker {
  private StaticChecker() {}

  public static Map<String, KlassDescriptor> produceClassDescriptor(Program program)
      throws SemanticException {
    distinctClassNameCheck(program);
    for (final var klass : program.klassList()) {
      distinctFieldNameCheck(klass);
      distinctMethodNameCheck(klass);
    }
    return program.klassList().stream()
        .collect(
            Collectors.toMap(
                k -> k.name().id(),
                k ->
                    new KlassDescriptor(
                        k.fields().stream()
                            .collect(Collectors.toMap(v -> v.name().id(), Var::type)),
                        k.methods().stream()
                            .collect(Collectors.groupingBy(m -> m.name().id()))
                            .entrySet()
                            .stream()
                            .collect(
                                Collectors.toMap(
                                    Map.Entry::getKey,
                                    e ->
                                        Multimaps.index(
                                            e.getValue().stream()
                                                .map(
                                                    m ->
                                                        new MethodDescriptor(
                                                            m.args().stream()
                                                                .map(Var::type)
                                                                .collect(
                                                                    Collectors
                                                                        .toUnmodifiableList()),
                                                            m.type()))
                                                .collect(Collectors.toUnmodifiableList()),
                                            md -> md.argTypes().size()))))));
  }

  private static void distinctClassNameCheck(Program program) throws SemanticException {
    final var grouped =
        program.klassList().stream().collect(Collectors.groupingBy(k -> k.name().id()));
    for (final var klassList : grouped.values()) {
      if (klassList.size() > 1) {
        throw new SemanticException(
            "Names of classes in a program must be distinct.",
            "duplicate class name",
            klassList.stream().map(Klass::name).collect(Collectors.toUnmodifiableList()));
      }
    }
  }

  private static void distinctFieldNameCheck(Klass klass) throws SemanticException {
    final var grouped = klass.fields().stream().collect(Collectors.groupingBy(v -> v.name().id()));
    for (final var fieldList : grouped.values()) {
      if (fieldList.size() > 1) {
        throw new SemanticException(
            "Names of fields in a class must be distinct.",
            "duplicate field name",
            fieldList.stream().map(Var::name).collect(Collectors.toUnmodifiableList()));
      }
    }
  }

  private static void distinctMethodNameCheck(Klass klass) throws SemanticException {
    final var grouped = klass.methods().stream().collect(Collectors.groupingBy(m -> m.name().id()));
    for (final var methodList : grouped.values()) {
      if (methodList.size() > 1) {
        final var signatureGroupedMethods =
            methodList.stream()
                .collect(
                    Collectors.groupingBy(
                        e ->
                            e.args().stream()
                                .map(Var::type)
                                .collect(Collectors.toUnmodifiableList())));
        for (final var signatureMethodList : signatureGroupedMethods.values()) {
          if (signatureMethodList.size() > 1) {
            throw new SemanticException(
                "Names of methods with the same signature in a class must be distinct.",
                "duplicate method name with same signature",
                signatureMethodList.stream()
                    .map(Method::name)
                    .collect(Collectors.toUnmodifiableList()));
          }
        }
      }
    }
  }
}
