package jlitec.ir3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.stmt.ReadlnStmt;
import jlitec.ir3.stmt.Stmt;

public class Ir3CodeGen {
  // prevent instantiation
  private Ir3CodeGen() {}

  private static record MethodDescriptor(
      String cname,
      String methodName,
      jlitec.ast.Type returnType,
      List<jlitec.ast.Type> argTypes) {}

  public static Program generate(jlitec.ast.Program program) {
    final var dataList =
        program.klassList().stream()
            .map(
                k ->
                    new Data(
                        k.cname(),
                        k.fields().stream().map(Var::new).collect(Collectors.toUnmodifiableList())))
            .collect(Collectors.toUnmodifiableList());
    final var mangledMethodNameMap = generateMangledMethodNames(program);
    // TODO generate code for program
    final var methodList = new ArrayList<Method>();
    for (final var klass : program.klassList()) {
      for (final var method : klass.methods()) {
        final var tempVars = new ArrayList<Var>();
        final var instructions = new ArrayList<Stmt>();

        // TODO generate instructions (and temp vars)
        for (final var stmt : method.stmtList()) {
          final Stmt genStmt =
              switch (stmt.getStmtType()) {
                case STMT_IF -> null;
                case STMT_WHILE -> null;
                case STMT_READLN -> {
                  final var rs = (jlitec.ast.stmt.ReadlnStmt) stmt;
                  yield new ReadlnStmt(new IdRvalExpr(rs.id()));
                }
                case STMT_PRINTLN -> null;
                case STMT_VAR_ASSIGN -> null;
                case STMT_FIELD_ASSIGN -> null;
                case STMT_CALL -> null;
                case STMT_RETURN -> null;
              };
          // TODO remove conditional
          if (genStmt != null) instructions.add(genStmt);
        }

        methodList.add(
            new Method(
                klass.cname(),
                Type.fromAst(method.returnType()),
                mangledMethodNameMap.get(
                    new MethodDescriptor(
                        klass.cname(), method.id(), method.returnType(), method.argTypes())),
                method.args().stream().map(Var::new).collect(Collectors.toUnmodifiableList()),
                Stream.concat(method.vars().stream().map(Var::new), tempVars.stream())
                    .collect(Collectors.toUnmodifiableList()),
                instructions));
      }
    }

    return new Program(dataList, methodList);
  }

  private static Map<MethodDescriptor, String> generateMangledMethodNames(
      jlitec.ast.Program program) {
    final var result = new HashMap<MethodDescriptor, String>();
    for (final var klass : program.klassList()) {
      int counter = 0;
      for (final var method : klass.methods()) {
        final var methodDescriptor =
            new MethodDescriptor(
                klass.cname(), method.id(), method.returnType(), method.argTypes());
        if (result.containsKey(methodDescriptor))
          throw new RuntimeException("Duplicate overloaded method detected.");
        result.put(
            methodDescriptor,
            method.id().equals("main") ? "main" : "%" + klass.cname() + "_" + counter);
        counter++;
      }
    }
    return result;
  }
}
