package jlitec.ir3.codegen;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.ast.expr.ExprType;
import jlitec.ir3.Data;
import jlitec.ir3.Ir3Type;
import jlitec.ir3.Method;
import jlitec.ir3.Program;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.expr.CallExpr;
import jlitec.ir3.expr.FieldExpr;
import jlitec.ir3.expr.NewExpr;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;
import jlitec.ir3.stmt.CallStmt;
import jlitec.ir3.stmt.PrintlnStmt;
import jlitec.ir3.stmt.ReadlnStmt;
import jlitec.ir3.stmt.ReturnStmt;
import jlitec.ir3.stmt.Stmt;
import jlitec.ir3.stmt.VarAssignStmt;

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
            method.vars().stream()
                .collect(
                    Collectors.toUnmodifiableMap(jlitec.ast.Var::id, v -> Type.fromAst(v.type())));
        final var fieldMap =
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
                  final var rvalChunk =
                      toRval(
                          ps.expr(),
                          klass.cname(),
                          mangledMethodNameMap,
                          localVarMap,
                          fieldMap,
                          tempVarGen);
                  yield ImmutableList.<Stmt>builder()
                      .addAll(rvalChunk.stmtList())
                      .add(new PrintlnStmt(rvalChunk.rval()))
                      .build();
                }
                case STMT_VAR_ASSIGN -> null;
                case STMT_FIELD_ASSIGN -> null;
                case STMT_CALL -> {
                  final var cs = (jlitec.ast.stmt.CallStmt) stmt;
                  final var methodReference = cs.methodReference();
                  final var mangledMethodName =
                      mangledMethodNameMap.get(
                          new MethodDescriptor(
                              methodReference.cname(),
                              methodReference.methodName(),
                              methodReference.returnType(),
                              methodReference.argTypes()));
                  final var target = new IdRvalExpr(mangledMethodName);
                  final var argsRvalChunk =
                      cs.args().stream()
                          .map(
                              e ->
                                  toRval(
                                      e,
                                      klass.cname(),
                                      mangledMethodNameMap,
                                      localVarMap,
                                      fieldMap,
                                      tempVarGen))
                          .collect(Collectors.toUnmodifiableList());
                  final var thisIdRvalExpr = resolveThis(cs.target(), tempVarGen);
                  final var argsRvalExpr =
                      Stream.concat(
                              Stream.of(thisIdRvalExpr.idRval()),
                              argsRvalChunk.stream().map(RvalChunk::rval))
                          .collect(Collectors.toUnmodifiableList());
                  final var additionalStmtList =
                      argsRvalChunk.stream()
                          .flatMap(i -> i.stmtList().stream())
                          .collect(Collectors.toUnmodifiableList());
                  yield ImmutableList.<Stmt>builder()
                      .addAll(additionalStmtList)
                      .addAll(thisIdRvalExpr.stmtList())
                      .add(new CallStmt(target, argsRvalExpr))
                      .build();
                }
                case STMT_RETURN -> {
                  final var rs = (jlitec.ast.stmt.ReturnStmt) stmt;
                  final var maybeIdRvalChunk =
                      rs.maybeExpr()
                          .map(
                              e ->
                                  toIdRval(
                                      e,
                                      klass.cname(),
                                      mangledMethodNameMap,
                                      localVarMap,
                                      fieldMap,
                                      tempVarGen));
                  if (maybeIdRvalChunk.isEmpty()) {
                    yield List.of(new ReturnStmt(Optional.empty()));
                  } else {
                    final var rvalChunk = maybeIdRvalChunk.get();
                    yield ImmutableList.<Stmt>builder()
                        .addAll(rvalChunk.stmtList())
                        .add(new ReturnStmt(Optional.of(rvalChunk.idRval())))
                        .build();
                  }
                }
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

  private static IdRvalChunk toIdRval(
      jlitec.ast.expr.Expr expr,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_INT_LITERAL -> {
        final var ile = (jlitec.ast.expr.IntLiteralExpr) expr;
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.INT));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalChunk(
            idRvalExpr, List.of(new VarAssignStmt(idRvalExpr, new IntRvalExpr(ile.value()))));
      }
      case EXPR_STRING_LITERAL -> {
        final var sle = (jlitec.ast.expr.StringLiteralExpr) expr;
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.STRING));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalChunk(
            idRvalExpr, List.of(new VarAssignStmt(idRvalExpr, new StringRvalExpr(sle.value()))));
      }
      case EXPR_BOOL_LITERAL -> {
        final var ble = (jlitec.ast.expr.BoolLiteralExpr) expr;
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalChunk(
            idRvalExpr, List.of(new VarAssignStmt(idRvalExpr, new BoolRvalExpr(ble.value()))));
      }
      case EXPR_BINARY -> null;
      case EXPR_UNARY -> null;
      case EXPR_DOT -> null;
      case EXPR_CALL -> {
        final var ce = (jlitec.ast.expr.CallExpr) expr;
        // call expression type should not be null
        final var tempVar = gen.gen(Type.fromTypeAnnotation(ce.typeAnnotation()).get());

        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        final var methodReference = ce.methodReference();
        final var mangledMethodName =
            mangledMethodNameMap.get(
                new MethodDescriptor(
                    methodReference.cname(),
                    methodReference.methodName(),
                    methodReference.returnType(),
                    methodReference.argTypes()));
        final var target = new IdRvalExpr(mangledMethodName);
        final var argsRvalChunk =
            ce.args().stream()
                .map(e -> toRval(e, cname, mangledMethodNameMap, localVarMap, fieldMap, gen))
                .collect(Collectors.toUnmodifiableList());
        final var thisIdRvalExpr = resolveThis(ce.target(), gen);
        final var argsRvalExpr =
            Stream.concat(
                    Stream.of(thisIdRvalExpr.idRval()), argsRvalChunk.stream().map(RvalChunk::rval))
                .collect(Collectors.toUnmodifiableList());
        final var additionalStmtList =
            argsRvalChunk.stream()
                .flatMap(i -> i.stmtList().stream())
                .collect(Collectors.toUnmodifiableList());
        yield new IdRvalChunk(
            idRvalExpr,
            ImmutableList.<Stmt>builder()
                .addAll(additionalStmtList)
                .addAll(thisIdRvalExpr.stmtList())
                .add(new VarAssignStmt(idRvalExpr, new CallExpr(target, argsRvalExpr)))
                .build());
      }
      case EXPR_THIS -> new IdRvalChunk(new IdRvalExpr("this"), List.of());
      case EXPR_ID -> {
        final var ie = (jlitec.ast.expr.IdExpr) expr;
        if (localVarMap.containsKey(ie.id())) {
          // local var
          yield new IdRvalChunk(new IdRvalExpr(ie.id()), List.of());
        } else {
          // a field in `this`
          final var tempVar = gen.gen(fieldMap.get(ie.id()));
          final var idRvalExpr = new IdRvalExpr(tempVar.id());
          yield new IdRvalChunk(
              idRvalExpr,
              List.of(
                  new VarAssignStmt(idRvalExpr, new FieldExpr(new IdRvalExpr("this"), ie.id()))));
        }
      }
      case EXPR_NEW -> {
        final var ne = (jlitec.ast.expr.NewExpr) expr;
        final var tempVar = gen.gen(new Type.KlassType(ne.cname()));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalChunk(
            idRvalExpr, List.of(new VarAssignStmt(idRvalExpr, new NewExpr(ne.cname()))));
      }
      case EXPR_NULL -> null;
    };
  }

  private static RvalChunk toRval(
      jlitec.ast.expr.Expr expr,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_INT_LITERAL -> new RvalChunk(
          new IntRvalExpr(((jlitec.ast.expr.IntLiteralExpr) expr).value()), List.of());
      case EXPR_STRING_LITERAL -> new RvalChunk(
          new StringRvalExpr(((jlitec.ast.expr.StringLiteralExpr) expr).value()), List.of());
      case EXPR_BOOL_LITERAL -> new RvalChunk(
          new BoolRvalExpr(((jlitec.ast.expr.BoolLiteralExpr) expr).value()), List.of());
      case EXPR_BINARY, EXPR_UNARY, EXPR_DOT, EXPR_CALL, EXPR_THIS, EXPR_ID, EXPR_NEW, EXPR_NULL -> {
        final var idRvalChunk =
            toIdRval(expr, cname, mangledMethodNameMap, fieldMap, localVarMap, gen);
        // TODO remove once done
        if (idRvalChunk == null) yield null;
        yield new RvalChunk(idRvalChunk.idRval(), idRvalChunk.stmtList());
      }
    };
  }

  private static IdRvalChunk resolveThis(jlitec.ast.expr.Expr target, TempVarGen gen) {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> throw new RuntimeException(
          "Trying to call non-callable expression.");
      case EXPR_ID -> {
        // local call
        yield new IdRvalChunk(new IdRvalExpr("this"), List.of());
      }
      case EXPR_DOT -> {
        // global call
        final var de = (jlitec.ast.expr.DotExpr) target;
        yield resolveDotExprThis(de.target(), gen);
      }
    };
  }

  private static IdRvalChunk resolveDotExprThis(jlitec.ast.expr.Expr target, TempVarGen gen) {
    if (target.getExprType() == ExprType.EXPR_ID) {
      final var ie = (jlitec.ast.expr.IdExpr) target;
      return new IdRvalChunk(new IdRvalExpr(ie.id()), List.of());
    }
    if (target.getExprType() == ExprType.EXPR_NEW) {
      final var ne = (jlitec.ast.expr.NewExpr) target;
      final var tempVar = gen.gen(Type.fromTypeAnnotation(ne.typeAnnotation()).get());
      final var idRvalExpr = new IdRvalExpr(tempVar.id());
      return new IdRvalChunk(
          idRvalExpr, List.of(new VarAssignStmt(idRvalExpr, new NewExpr(ne.cname()))));
    }
    final var de = (jlitec.ast.expr.DotExpr) target;
    final var idRvalChunk = resolveDotExprThis(de.target(), gen);
    // type annotation should not be null at this stage.
    final var tempVar = gen.gen(Type.fromTypeAnnotation(de.typeAnnotation()).get());
    final var idRvalExpr = new IdRvalExpr(tempVar.id());
    return new IdRvalChunk(
        idRvalExpr,
        ImmutableList.<Stmt>builder()
            .addAll(idRvalChunk.stmtList())
            .add(new VarAssignStmt(idRvalExpr, new FieldExpr(idRvalChunk.idRval(), de.id())))
            .build());
  }

  private static Map<MethodDescriptor, String> generateMangledMethodNames(
      jlitec.ast.Program program) {
    final var result = new HashMap<MethodDescriptor, String>();
    for (final var klass : program.klassList()) {
      int counter = 0;
      for (final var method : klass.methods()) {
        final var argTypes =
            method.args().stream()
                .map(jlitec.ast.Var::type)
                .map(Type::fromAst)
                .collect(Collectors.toUnmodifiableList());
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
    return Collections.unmodifiableMap(result);
  }
}
