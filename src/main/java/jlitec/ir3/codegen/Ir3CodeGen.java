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
import jlitec.ast.expr.DotExpr;
import jlitec.ir3.Data;
import jlitec.ir3.Ir3Type;
import jlitec.ir3.Method;
import jlitec.ir3.Program;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
import jlitec.ir3.expr.BinaryExpr;
import jlitec.ir3.expr.BinaryOp;
import jlitec.ir3.expr.CallExpr;
import jlitec.ir3.expr.FieldExpr;
import jlitec.ir3.expr.NewExpr;
import jlitec.ir3.expr.UnaryExpr;
import jlitec.ir3.expr.UnaryOp;
import jlitec.ir3.expr.rval.BoolRvalExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;
import jlitec.ir3.expr.rval.IntRvalExpr;
import jlitec.ir3.expr.rval.NullRvalExpr;
import jlitec.ir3.expr.rval.StringRvalExpr;
import jlitec.ir3.stmt.CallStmt;
import jlitec.ir3.stmt.CmpStmt;
import jlitec.ir3.stmt.FieldAssignStmt;
import jlitec.ir3.stmt.GotoStmt;
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
    final var labelGen = new LabelGen();
    for (final var klass : program.klassList()) {
      for (final var method : klass.methods()) {
        final var tempVarGen = new TempVarGen();
        final var localVarMap =
            Stream.concat(method.vars().stream(), method.args().stream())
                .collect(
                    Collectors.toUnmodifiableMap(jlitec.ast.Var::id, v -> Type.fromAst(v.type())));
        final var fieldMap =
            klass.fields().stream()
                .collect(
                    Collectors.toUnmodifiableMap(jlitec.ast.Var::id, v -> Type.fromAst(v.type())));
        final var instructions =
            method.stmtList().stream()
                .flatMap(
                    stmt ->
                        genStmt(
                            stmt,
                            klass,
                            mangledMethodNameMap,
                            localVarMap,
                            fieldMap,
                            tempVarGen,
                            labelGen)
                            .stream())
                .collect(Collectors.toUnmodifiableList());

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

  private static List<Stmt> genStmt(
      jlitec.ast.stmt.Stmt stmt,
      jlitec.ast.Klass klass,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen tempVarGen,
      LabelGen labelGen) {
    return switch (stmt.getStmtType()) {
      case STMT_IF -> {
        final var is = (jlitec.ast.stmt.IfStmt) stmt;
        final var conditionChunk =
            toRelExpr(
                is.condition(),
                klass.cname(),
                mangledMethodNameMap,
                localVarMap,
                fieldMap,
                tempVarGen);
        final var trueLabel = labelGen.gen();
        final var nextLabel = labelGen.gen();
        final var trueStmtList =
            is.thenStmtList().stream()
                .flatMap(
                    s ->
                        genStmt(
                            s,
                            klass,
                            mangledMethodNameMap,
                            localVarMap,
                            fieldMap,
                            tempVarGen,
                            labelGen)
                            .stream())
                .collect(Collectors.toUnmodifiableList());
        final var elseStmtList =
            is.elseStmtList().stream()
                .flatMap(
                    s ->
                        genStmt(
                            s,
                            klass,
                            mangledMethodNameMap,
                            localVarMap,
                            fieldMap,
                            tempVarGen,
                            labelGen)
                            .stream())
                .collect(Collectors.toUnmodifiableList());
        yield ImmutableList.<Stmt>builder()
            .addAll(conditionChunk.stmtList())
            .add(new CmpStmt(conditionChunk.expr(), trueLabel))
            .addAll(elseStmtList)
            .add(new GotoStmt(nextLabel))
            .add(trueLabel)
            .addAll(trueStmtList)
            .add(nextLabel)
            .build();
      }
      case STMT_WHILE -> {
        final var ws = (jlitec.ast.stmt.WhileStmt) stmt;
        final var conditionChunk =
            toOppositeRelExpr(
                ws.condition(),
                klass.cname(),
                mangledMethodNameMap,
                localVarMap,
                fieldMap,
                tempVarGen);
        final var beginLabel = labelGen.gen();
        final var nextLabel = labelGen.gen();
        final var stmtList =
            ws.stmtList().stream()
                .flatMap(
                    s ->
                        genStmt(
                            s,
                            klass,
                            mangledMethodNameMap,
                            localVarMap,
                            fieldMap,
                            tempVarGen,
                            labelGen)
                            .stream())
                .collect(Collectors.toUnmodifiableList());
        yield ImmutableList.<Stmt>builder()
            .add(beginLabel)
            .addAll(conditionChunk.stmtList())
            .add(new CmpStmt(conditionChunk.expr(), nextLabel))
            .addAll(stmtList)
            .add(new GotoStmt(beginLabel))
            .add(nextLabel)
            .build();
      }
      case STMT_READLN -> {
        final var rs = (jlitec.ast.stmt.ReadlnStmt) stmt;
        yield List.of(new ReadlnStmt(new IdRvalExpr(rs.id())));
      }
      case STMT_PRINTLN -> {
        final var ps = (jlitec.ast.stmt.PrintlnStmt) stmt;
        final var rvalChunk =
            toRval(
                ps.expr(), klass.cname(), mangledMethodNameMap, localVarMap, fieldMap, tempVarGen);
        yield ImmutableList.<Stmt>builder()
            .addAll(rvalChunk.stmtList())
            .add(new PrintlnStmt(rvalChunk.rval()))
            .build();
      }
      case STMT_VAR_ASSIGN -> {
        final var vas = (jlitec.ast.stmt.VarAssignStmt) stmt;
        final var rhsChunk =
            toExpr(
                vas.rhs(), klass.cname(), mangledMethodNameMap, localVarMap, fieldMap, tempVarGen);
        final Stmt genStmt;
        if (localVarMap.containsKey(vas.lhsId())) {
          // local var
          genStmt = new VarAssignStmt(new IdRvalExpr(vas.lhsId()), rhsChunk.expr());
        } else {
          // a field in `this`
          genStmt = new FieldAssignStmt(new IdRvalExpr("this"), vas.lhsId(), rhsChunk.expr());
        }
        yield ImmutableList.<Stmt>builder().addAll(rhsChunk.stmtList()).add(genStmt).build();
      }
      case STMT_FIELD_ASSIGN -> {
        final var fas = (jlitec.ast.stmt.FieldAssignStmt) stmt;
        final var lhsTarget =
            toIdRval(
                fas.lhsTarget(),
                klass.cname(),
                mangledMethodNameMap,
                localVarMap,
                fieldMap,
                tempVarGen);
        final var rhs =
            toExpr(
                fas.rhs(), klass.cname(), mangledMethodNameMap, localVarMap, fieldMap, tempVarGen);
        yield ImmutableList.<Stmt>builder()
            .addAll(lhsTarget.stmtList())
            .addAll(rhs.stmtList())
            .add(new FieldAssignStmt(lhsTarget.idRval(), fas.lhsId(), rhs.expr()))
            .build();
      }
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
        final var thisIdRvalExpr =
            resolveThis(
                cs.target(),
                klass.cname(),
                mangledMethodNameMap,
                localVarMap,
                fieldMap,
                tempVarGen);
        final var argsRvalExpr =
            Stream.concat(
                    Stream.of(thisIdRvalExpr.idRval()), argsRvalChunk.stream().map(RvalChunk::rval))
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
        if (rs.maybeExpr().isEmpty()) {
          yield List.of(new ReturnStmt(Optional.empty()));
        } else {
          final var idRvalChunk =
              toIdRval(
                  rs.maybeExpr().get(),
                  klass.cname(),
                  mangledMethodNameMap,
                  localVarMap,
                  fieldMap,
                  tempVarGen);
          yield ImmutableList.<Stmt>builder()
              .addAll(idRvalChunk.stmtList())
              .add(new ReturnStmt(Optional.of(idRvalChunk.idRval())))
              .build();
        }
      }
    };
  }

  private static ExprChunk toOppositeRelExpr(
      jlitec.ast.expr.Expr expr,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_BOOL_LITERAL -> new ExprChunk(
          new BoolRvalExpr(!((jlitec.ast.expr.BoolLiteralExpr) expr).value()), List.of());
      case EXPR_BINARY -> {
        final var be = (jlitec.ast.expr.BinaryExpr) expr;
        final var op = be.op();
        yield switch (op) {
          case LT, GT, LEQ, GEQ, EQ, NEQ -> {
            final var oppositeOp =
                switch (op) {
                  case LT -> BinaryOp.GEQ;
                  case GT -> BinaryOp.LEQ;
                  case LEQ -> BinaryOp.GT;
                  case GEQ -> BinaryOp.LT;
                  case EQ -> BinaryOp.NEQ;
                  case NEQ -> BinaryOp.EQ;
                  case OR, AND, PLUS, MINUS, MULT, DIV -> throw new RuntimeException(
                      "This is literally impossible to reach in toOppositeRelExpr");
                };
            final var lhs =
                toRval(be.lhs(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
            final var rhs =
                toRval(be.rhs(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
            yield new ExprChunk(
                new BinaryExpr(oppositeOp, lhs.rval(), rhs.rval()),
                ImmutableList.<Stmt>builder()
                    .addAll(lhs.stmtList())
                    .addAll(rhs.stmtList())
                    .build());
          }
          case OR, AND -> {
            final var rvalChunk =
                toRval(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
            final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
            final var idRvalExpr = new IdRvalExpr(tempVar.id());
            yield new ExprChunk(
                idRvalExpr,
                ImmutableList.<Stmt>builder()
                    .addAll(rvalChunk.stmtList())
                    .add(
                        new VarAssignStmt(idRvalExpr, new UnaryExpr(UnaryOp.NOT, rvalChunk.rval())))
                    .build());
          }
          case PLUS, MINUS, MULT, DIV -> throw new RuntimeException(
              "typecheck failure at toOppositeRelExpr");
        };
      }
      case EXPR_UNARY, EXPR_DOT, EXPR_CALL -> {
        final var rvalChunk = toRval(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        final var tempVar = gen.gen(new Type.PrimitiveType(Ir3Type.BOOL));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new ExprChunk(
            idRvalExpr,
            ImmutableList.<Stmt>builder()
                .addAll(rvalChunk.stmtList())
                .add(new VarAssignStmt(idRvalExpr, new UnaryExpr(UnaryOp.NOT, rvalChunk.rval())))
                .build());
      }
      case EXPR_ID -> new ExprChunk(
          new IdRvalExpr(((jlitec.ast.expr.IdExpr) expr).id()), List.of());
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_NEW, EXPR_NULL, EXPR_THIS -> {
        // this should not have passed typechecking
        throw new RuntimeException("non-boolean type passed to `toRelExpr");
      }
    };
  }

  private static ExprChunk toRelExpr(
      jlitec.ast.expr.Expr expr,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_BOOL_LITERAL -> new ExprChunk(
          new BoolRvalExpr(((jlitec.ast.expr.BoolLiteralExpr) expr).value()), List.of());
      case EXPR_BINARY -> {
        final var be = (jlitec.ast.expr.BinaryExpr) expr;
        final var op = be.op();
        yield switch (op) {
          case LT, GT, LEQ, GEQ, EQ, NEQ -> {
            final var lhs =
                toRval(be.lhs(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
            final var rhs =
                toRval(be.rhs(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
            yield new ExprChunk(
                new BinaryExpr(BinaryOp.fromAst(op), lhs.rval(), rhs.rval()),
                ImmutableList.<Stmt>builder()
                    .addAll(lhs.stmtList())
                    .addAll(rhs.stmtList())
                    .build());
          }
          case OR, AND -> {
            final var rvalChunk =
                toRval(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
            yield new ExprChunk(rvalChunk.rval(), rvalChunk.stmtList());
          }
          case PLUS, MINUS, MULT, DIV -> throw new RuntimeException(
              "typecheck failure at toRelExpr");
        };
      }
      case EXPR_UNARY, EXPR_DOT, EXPR_CALL -> {
        final var rvalChunk = toRval(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        yield new ExprChunk(rvalChunk.rval(), rvalChunk.stmtList());
      }
      case EXPR_ID -> new ExprChunk(
          new IdRvalExpr(((jlitec.ast.expr.IdExpr) expr).id()), List.of());
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_NEW, EXPR_NULL, EXPR_THIS -> {
        // this should not have passed typechecking
        throw new RuntimeException("non-boolean type passed to `toRelExpr");
      }
    };
  }

  private static ExprChunk toExpr(
      jlitec.ast.expr.Expr expr,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_BOOL_LITERAL, EXPR_STRING_LITERAL, EXPR_INT_LITERAL, EXPR_NULL -> {
        // delegate to `toRval`
        final var rvalChunk = toRval(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        yield new ExprChunk(rvalChunk.rval(), rvalChunk.stmtList());
      }
      case EXPR_BINARY -> {
        final var be = (jlitec.ast.expr.BinaryExpr) expr;
        final var lhsChunk =
            toRval(be.lhs(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        final var rhsChunk =
            toRval(be.rhs(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        yield new ExprChunk(
            new BinaryExpr(BinaryOp.fromAst(be.op()), lhsChunk.rval(), rhsChunk.rval()),
            ImmutableList.<Stmt>builder()
                .addAll(lhsChunk.stmtList())
                .addAll(rhsChunk.stmtList())
                .build());
      }
      case EXPR_UNARY -> {
        final var ue = (jlitec.ast.expr.UnaryExpr) expr;
        final var rvalChunk =
            toRval(ue.expr(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        yield new ExprChunk(
            new UnaryExpr(UnaryOp.fromAst(ue.op()), rvalChunk.rval()), rvalChunk.stmtList());
      }
      case EXPR_DOT -> {
        final var de = (jlitec.ast.expr.DotExpr) expr;
        final var idRvalChunk =
            toIdRval(de.target(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        yield new ExprChunk(new FieldExpr(idRvalChunk.idRval(), de.id()), idRvalChunk.stmtList());
      }
      case EXPR_CALL -> {
        final var ce = (jlitec.ast.expr.CallExpr) expr;
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
        final var thisIdRvalExpr =
            resolveThis(ce.target(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        final var argsRvalExpr =
            Stream.concat(
                    Stream.of(thisIdRvalExpr.idRval()), argsRvalChunk.stream().map(RvalChunk::rval))
                .collect(Collectors.toUnmodifiableList());
        final var additionalStmtList =
            argsRvalChunk.stream()
                .flatMap(i -> i.stmtList().stream())
                .collect(Collectors.toUnmodifiableList());
        yield new ExprChunk(
            new CallExpr(target, argsRvalExpr),
            ImmutableList.<Stmt>builder()
                .addAll(additionalStmtList)
                .addAll(thisIdRvalExpr.stmtList())
                .build());
      }
      case EXPR_ID -> {
        final var ie = (jlitec.ast.expr.IdExpr) expr;
        if (localVarMap.containsKey(ie.id())) {
          // local var
          yield new ExprChunk(new IdRvalExpr(ie.id()), List.of());
        } else {
          // a field in `this`
          yield new ExprChunk(new FieldExpr(new IdRvalExpr("this"), ie.id()), List.of());
        }
      }
      case EXPR_THIS -> new ExprChunk(new IdRvalExpr("this"), List.of());
      case EXPR_NEW -> new ExprChunk(
          new NewExpr(((jlitec.ast.expr.NewExpr) expr).cname()), List.of());
    };
  }

  private static IdRvalChunk toIdRval(
      jlitec.ast.expr.Expr expr,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (expr.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_NEW, EXPR_CALL, EXPR_DOT -> {
        // delegate to `toExpr` and wrap with a temp var.
        final var exprChunk = toExpr(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        final var tempVar = gen.gen(Type.fromTypeAnnotation(expr.typeAnnotation()));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalChunk(
            idRvalExpr,
            ImmutableList.<Stmt>builder()
                .addAll(exprChunk.stmtList())
                .add(new VarAssignStmt(idRvalExpr, exprChunk.expr()))
                .build());
      }
      case EXPR_ID -> {
        final var ie = (jlitec.ast.expr.IdExpr) expr;
        if (localVarMap.containsKey(ie.id())) {
          // local var
          yield new IdRvalChunk(new IdRvalExpr(ie.id()), List.of());
        } else {
          // a field in `this`
          final var tempVar = gen.gen(Type.fromTypeAnnotation(expr.typeAnnotation()));
          final var idRvalExpr = new IdRvalExpr(tempVar.id());
          yield new IdRvalChunk(
              idRvalExpr,
              List.of(
                  new VarAssignStmt(idRvalExpr, new FieldExpr(new IdRvalExpr("this"), ie.id()))));
        }
      }
      case EXPR_THIS -> new IdRvalChunk(new IdRvalExpr("this"), List.of());
      case EXPR_NULL -> throw new RuntimeException("It is not possible to create idRval for null.");
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
      case EXPR_NULL -> new RvalChunk(new NullRvalExpr(), List.of());
      case EXPR_BINARY, EXPR_UNARY, EXPR_DOT, EXPR_CALL, EXPR_THIS, EXPR_ID, EXPR_NEW -> {
        // delegate to `toIdRval`
        final var idRvalChunk =
            toIdRval(expr, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        yield new RvalChunk(idRvalChunk.idRval(), idRvalChunk.stmtList());
      }
    };
  }

  private static IdRvalChunk resolveThis(
      jlitec.ast.expr.Expr target,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> throw new RuntimeException(
          "Trying to call non-callable expression.");
        // local call
      case EXPR_ID -> new IdRvalChunk(new IdRvalExpr("this"), List.of());
        // global call
      case EXPR_DOT -> resolveDotExprThis(
          ((jlitec.ast.expr.DotExpr) target).target(),
          cname,
          mangledMethodNameMap,
          localVarMap,
          fieldMap,
          gen);
    };
  }

  private static IdRvalChunk resolveDotExprThis(
      jlitec.ast.expr.Expr target,
      String cname,
      Map<MethodDescriptor, String> mangledMethodNameMap,
      Map<String, Type> localVarMap,
      Map<String, Type> fieldMap,
      TempVarGen gen) {
    return switch (target.getExprType()) {
      case EXPR_ID, EXPR_NEW, EXPR_CALL, EXPR_THIS -> toIdRval(
          target, cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_NULL -> throw new RuntimeException(
          "Invalid target");
      case EXPR_DOT -> {
        final var de = (DotExpr) target;
        final var idRvalChunk =
            resolveDotExprThis(
                de.target(), cname, mangledMethodNameMap, localVarMap, fieldMap, gen);
        final var tempVar = gen.gen(Type.fromTypeAnnotation(de.typeAnnotation()));
        final var idRvalExpr = new IdRvalExpr(tempVar.id());
        yield new IdRvalChunk(
            idRvalExpr,
            ImmutableList.<Stmt>builder()
                .addAll(idRvalChunk.stmtList())
                .add(new VarAssignStmt(idRvalExpr, new FieldExpr(idRvalChunk.idRval(), de.id())))
                .build());
      }
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
        if (result.containsKey(methodDescriptor)) {
          throw new RuntimeException("Duplicate overloaded method detected.");
        }
        result.put(
            methodDescriptor,
            method.id().equals("main") ? "main" : "%" + klass.cname() + "_" + counter);
        counter++;
      }
    }
    return Collections.unmodifiableMap(result);
  }
}
