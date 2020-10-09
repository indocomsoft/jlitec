package jlitec.ir3.codegen;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.ast.expr.BoolLiteralExpr;
import jlitec.ast.expr.IdExpr;
import jlitec.ast.expr.IntLiteralExpr;
import jlitec.ast.expr.StringLiteralExpr;
import jlitec.ir3.Data;
import jlitec.ir3.Method;
import jlitec.ir3.Program;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;
import jlitec.ir3.stmt.PrintlnStmt;
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
    final var methodList = new ArrayList<Method>();
    for (final var klass : program.klassList()) {
      for (final var method : klass.methods()) {
        final var tempVarGen = new TempVarGen();
        final var instructions = new ArrayList<Stmt>();
        final var localVarMap =
            klass.fields().stream()
                .collect(
                    Collectors.toUnmodifiableMap(jlitec.ast.Var::id, v -> Type.fromAst(v.type())));

        // TODO generate instructions (and temp vars)
        for (final var stmt : method.stmtList()) {
          final List<Stmt> genStmts =
              switch (stmt.getStmtType()) {
                case STMT_IF -> null;
                case STMT_WHILE -> null;
                case STMT_READLN -> {
                  final var rs = (jlitec.ast.stmt.ReadlnStmt) stmt;
                  yield List.of(new ReadlnStmt(new IdRvalExpr(rs.id())));
                }
                case STMT_PRINTLN -> {
                  final var ps = (jlitec.ast.stmt.PrintlnStmt) stmt;
                  final var rvalChunk = toRval(ps.expr(), localVarMap, tempVarGen);
                  yield ImmutableList.<Stmt>builder()
                      .addAll(rvalChunk.stmtList())
                      .add(new PrintlnStmt(rvalChunk.rval()))
                      .build();
                }
                case STMT_VAR_ASSIGN -> null;
                case STMT_FIELD_ASSIGN -> null;
                case STMT_CALL -> null;
                case STMT_RETURN -> null;
              };
          // TODO remove conditional
          if (genStmts != null) instructions.addAll(genStmts);
        }

        methodList.add(
            new Method(
                klass.cname(),
                Type.fromAst(method.returnType()),
                mangledMethodNameMap.get(
                    new MethodDescriptor(
                        klass.cname(), method.id(), method.returnType(), method.argTypes())),
                method.args().stream().map(Var::new).collect(Collectors.toUnmodifiableList()),
                Stream.concat(method.vars().stream().map(Var::new), tempVarGen.getVars().stream())
                    .collect(Collectors.toUnmodifiableList()),
                instructions));
      }
    }

    return new Program(dataList, methodList);
  }

  private static RvalChunk toRval(
      jlitec.ast.expr.Expr expr, Map<String, Type> localVarMap, TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_INT_LITERAL -> new RvalChunk(
          new IntRvalExpr(((IntLiteralExpr) expr).value()), List.of());
      case EXPR_STRING_LITERAL -> new RvalChunk(
          new StringRvalExpr(((StringLiteralExpr) expr).value()), List.of());
      case EXPR_BOOL_LITERAL -> new RvalChunk(
          new BoolRvalExpr(((BoolLiteralExpr) expr).value()), List.of());
      case EXPR_BINARY -> null;
      case EXPR_UNARY -> null;
      case EXPR_DOT -> null;
      case EXPR_CALL -> null;
      case EXPR_THIS -> null;
      case EXPR_ID -> new RvalChunk(new IdRvalExpr(((IdExpr) expr).id()), List.of());
      case EXPR_NEW -> null;
      case EXPR_NULL -> null;
    };
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
