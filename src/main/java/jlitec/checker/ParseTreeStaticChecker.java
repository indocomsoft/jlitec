package jlitec.checker;

import com.google.common.collect.Multimaps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jlitec.parsetree.Klass;
import jlitec.parsetree.KlassType;
import jlitec.parsetree.Method;
import jlitec.parsetree.Program;
import jlitec.parsetree.Var;

public class ParseTreeStaticChecker {
  private ParseTreeStaticChecker() {}

  /**
   * Performs distinct name checking, and then produces class descriptors.
   * @param program the program
   * @return the map of class desriptors
   * @throws SemanticException when there's a semantic error in the program.
   */
  public static Map<String, KlassDescriptor> produceClassDescriptor(Program program) throws SemanticException {
    distinctNameCheck(program);
    final var result = program.klassList().stream()
        .collect(
            Collectors.toMap(
                k -> k.name().id(),
                k ->
                    new KlassDescriptor(
                        k.fields().stream()
                            .collect(Collectors.toMap(v -> v.name().id(), v -> new Type.Basic(v.type()))),
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
                                                    .map(Type.Method::new)
                                                .collect(Collectors.toUnmodifiableList()),
                                            md -> md.args().size()))))));
    cnameTypeExistenceCheck(program, result);
    return result;
  }

  public static void cnameTypeExistenceCheck(Program program, Map<String, KlassDescriptor> klassDescriptorMap) throws SemanticException {
    for (final var klass: program.klassList()) {
      // check fields
      for (final var field: klass.fields()) {
        final var type = field.type();
        if (type instanceof KlassType klassType && !klassDescriptorMap.containsKey(klassType.cname())) {
          throw new SemanticException("Non-existent class type `" + klass.name().id() + "'", "non-existent class type", List.of(type));
        }
      }
      // check methods
      for (final var method: klass.methods()) {
        // check return type
        final var returnType = method.type();
        if (returnType instanceof KlassType klassType && !klassDescriptorMap.containsKey(klassType.cname())) {
          throw new SemanticException("Non-existent class type `" + klass.name().id() + "'", "non-existent class type", List.of(returnType));
        }
        // check args
        for (final var arg: method.args()) {
          final var type = arg.type();
          if (type instanceof KlassType klassType && !klassDescriptorMap.containsKey(klassType.cname())) {
            throw new SemanticException("Non-existent class type `" + klass.name().id() + "'", "non-existent class type", List.of(type));
          }
        }
        // check vars
        for (final var variable: method.vars()) {
          final var type = variable.type();
          if (type instanceof KlassType klassType && !klassDescriptorMap.containsKey(klassType.cname())) {
            throw new SemanticException("Non-existent class type `" + klass.name().id() + "'", "non-existent class type", List.of(type));
          }
        }
      }
    }
  }

//  public static void typecheck(Program program, Map<String, KlassDescriptor> klassDescriptors) throws SemanticException {
//    Environment env = new Environment();
//    for (final var klass: program.klassList()) {
//      typecheck(klass, klassDescriptors.get(klass.name().id()), env);
//    }
//  }
//
//  public static void typecheck(Klass klass, KlassDescriptor klassDescriptor, Environment env) throws SemanticException {
//    env = env.augment(new Environment(klassDescriptor));
//    env = env.augment("this", new Type.Basic(new KlassType(klass.name().id(), klass.leftLocation(), klass.rightLocation())));
//
//  }
//
  private static void distinctNameCheck(Program program) throws SemanticException {
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
