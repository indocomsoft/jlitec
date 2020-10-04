package jlitec.checker;

import com.google.common.collect.Multimaps;
import java.util.Map;
import java.util.stream.Collectors;
import jlitec.parsetree.Klass;
import jlitec.parsetree.Method;
import jlitec.parsetree.Program;
import jlitec.parsetree.Var;

public class ParseTreeStaticChecker {
  private ParseTreeStaticChecker() {}

  public static Map<String, KlassDescriptor> produceClassDescriptor(Program program) {
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

  public static void distinctNameCheck(Program program) throws SemanticException {
    distinctClassNameCheck(program);
    for (final var klass : program.klassList()) {
      distinctFieldNameCheck(klass);
      distinctMethodNameCheck(klass);
    }
  }

  private static void distinctClassNameCheck(Program program) throws SemanticException {
    final var grouped =
        program.klassList().stream().collect(Collectors.groupingBy(k -> k.name().id()));
    for (final var entry : grouped.entrySet()) {
      final var klassList = entry.getValue();
      if (klassList.size() > 1) {
        throw new SemanticException(
            "Names of classes in a program must be distinct: class `" + entry.getKey() + "'.",
            "duplicate class name",
            klassList.stream().map(Klass::name).collect(Collectors.toUnmodifiableList()));
      }
    }
  }

  private static void distinctFieldNameCheck(Klass klass) throws SemanticException {
    final var grouped = klass.fields().stream().collect(Collectors.groupingBy(v -> v.name().id()));
    for (final var entry : grouped.entrySet()) {
      final var fieldList = entry.getValue();
      if (fieldList.size() > 1) {
        throw new SemanticException(
            "Names of fields in a class must be distinct: field `"
                + entry.getKey()
                + "', in class `"
                + klass.name().id()
                + "'.",
            "duplicate field name",
            fieldList.stream().map(Var::name).collect(Collectors.toUnmodifiableList()));
      }
    }
  }

  private static void distinctMethodNameCheck(Klass klass) throws SemanticException {
    final var grouped = klass.methods().stream().collect(Collectors.groupingBy(m -> m.name().id()));
    for (final var entry : grouped.entrySet()) {
      final var methodList = entry.getValue();
      if (methodList.size() > 1) {
        final var signatureGroupedMethods =
            methodList.stream()
                .collect(
                    Collectors.groupingBy(
                        e ->
                            e.args().stream()
                                .map(
                                    v ->
                                        v.type()
                                            .print(0)) // the printed form should uniquely identify
                                .collect(Collectors.toUnmodifiableList())));
        for (final var signatureEntry : signatureGroupedMethods.entrySet()) {
          final var signatureMethodList = signatureEntry.getValue();
          if (signatureMethodList.size() > 1) {
            final var signature =
                entry.getKey() + "(" + String.join(", ", signatureEntry.getKey()) + ")";
            throw new SemanticException(
                "Names of methods with the same signature in a class must be distinct: method `"
                    + signature
                    + "', in class `"
                    + klass.name().id()
                    + "'.",
                "duplicate method name with same signature: " + signature,
                signatureMethodList.stream()
                    .map(Method::name)
                    .collect(Collectors.toUnmodifiableList()));
          }
        }
      }
    }
  }
}
