package jlitec.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jlitec.ast.MethodReference;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.BinaryOp;
import jlitec.ast.expr.ThisExpr;
import jlitec.ast.expr.UnaryOp;
import jlitec.parsetree.JliteType;
import jlitec.parsetree.Klass;
import jlitec.parsetree.KlassType;
import jlitec.parsetree.Method;
import jlitec.parsetree.Program;
import jlitec.parsetree.Var;
import jlitec.parsetree.expr.BinaryExpr;
import jlitec.parsetree.expr.BoolLiteralExpr;
import jlitec.parsetree.expr.CallExpr;
import jlitec.parsetree.expr.DotExpr;
import jlitec.parsetree.expr.Expr;
import jlitec.parsetree.expr.IdExpr;
import jlitec.parsetree.expr.IntLiteralExpr;
import jlitec.parsetree.expr.NewExpr;
import jlitec.parsetree.expr.ParenExpr;
import jlitec.parsetree.expr.StringLiteralExpr;
import jlitec.parsetree.expr.UnaryExpr;
import jlitec.parsetree.stmt.CallStmt;
import jlitec.parsetree.stmt.FieldAssignStmt;
import jlitec.parsetree.stmt.IfStmt;
import jlitec.parsetree.stmt.PrintlnStmt;
import jlitec.parsetree.stmt.ReadlnStmt;
import jlitec.parsetree.stmt.ReturnStmt;
import jlitec.parsetree.stmt.Stmt;
import jlitec.parsetree.stmt.VarAssignStmt;
import jlitec.parsetree.stmt.WhileStmt;

public class ParseTreeStaticChecker {
  private ParseTreeStaticChecker() {}

  /**
   * Performs distinct name checking, and then produces class descriptors.
   *
   * @param program the program
   * @return the map of class desriptors
   * @throws SemanticException when there's a semantic error in the program.
   */
  public static Map<String, KlassDescriptor> produceClassDescriptor(Program program)
      throws SemanticException {
    distinctNameCheck(program);
    final var result =
        program.klassList().stream()
            .collect(
                Collectors.toMap(
                    k -> k.name().id(),
                    k ->
                        new KlassDescriptor(
                            k.fields().stream()
                                .collect(
                                    Collectors.toMap(
                                        v -> v.name().id(),
                                        v -> new Type.Basic(v.type().print(0)))),
                            k.methods().stream()
                                .collect(Collectors.groupingBy(m -> m.name().id()))
                                .entrySet()
                                .stream()
                                .collect(
                                    Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        e ->
                                            e.getValue().stream()
                                                .map(
                                                    m ->
                                                        new KlassDescriptor.MethodDescriptor(
                                                            new Type.Basic(m.type().print(0)),
                                                            m.args().stream()
                                                                .map(
                                                                    v ->
                                                                        new Type.Basic(
                                                                            v.type().print(0)))
                                                                .collect(
                                                                    Collectors
                                                                        .toUnmodifiableList())))
                                                .collect(Collectors.toUnmodifiableList()))))));
    cnameTypeExistenceCheck(program, result);
    return result;
  }

  private static void cnameTypeExistenceCheck(
      Program program, Map<String, KlassDescriptor> klassDescriptorMap) throws SemanticException {
    for (final var klass : program.klassList()) {
      // check fields
      for (final var field : klass.fields()) {
        final var type = field.type();
        if (type instanceof KlassType klassType
            && !klassDescriptorMap.containsKey(klassType.cname())) {
          throw new SemanticException(
              "Non-existent class type `" + klass.name().id() + "'",
              "non-existent class type",
              List.of(type));
        }
      }
      // check methods
      for (final var method : klass.methods()) {
        // check return type
        final var returnType = method.type();
        if (returnType instanceof KlassType klassType
            && !klassDescriptorMap.containsKey(klassType.cname())) {
          throw new SemanticException(
              "Non-existent class type `" + klass.name().id() + "'",
              "non-existent class type",
              List.of(returnType));
        }
        // check args
        for (final var arg : method.args()) {
          final var type = arg.type();
          if (type instanceof KlassType klassType
              && !klassDescriptorMap.containsKey(klassType.cname())) {
            throw new SemanticException(
                "Non-existent class type `" + klass.name().id() + "'",
                "non-existent class type",
                List.of(type));
          }
        }
        // check vars
        for (final var variable : method.vars()) {
          final var type = variable.type();
          if (type instanceof KlassType klassType
              && !klassDescriptorMap.containsKey(klassType.cname())) {
            throw new SemanticException(
                "Non-existent class type `" + klass.name().id() + "'",
                "non-existent class type",
                List.of(type));
          }
        }
      }
    }
  }

  public static jlitec.ast.Program toAst(
      jlitec.parsetree.Program program, Map<String, KlassDescriptor> klassDescriptorMap) {
    final Environment env = new Environment();
    final var astKlasses =
        program.klassList().stream()
            .map(k -> toAst(k, klassDescriptorMap, env))
            .collect(Collectors.toUnmodifiableList());
    return new jlitec.ast.Program(astKlasses);
  }

  private static jlitec.ast.Klass toAst(
      jlitec.parsetree.Klass klass,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env) {
    final var klassName = klass.name().id();
    env = env.augment(new Environment(klassDescriptorMap.get(klassName)));
    final var finalEnv = env.augment("this", new Type.Basic(klassName));
    final var astFields =
        klass.fields().stream().map(jlitec.ast.Var::new).collect(Collectors.toUnmodifiableList());
    final var astMethods =
        klass.methods().stream()
            .map(m -> toAst(m, klassDescriptorMap, finalEnv))
            .collect(Collectors.toUnmodifiableList());
    return new jlitec.ast.Klass(klassName, astFields, astMethods);
  }

  private static jlitec.ast.Method toAst(
      jlitec.parsetree.Method method,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env) {
    for (final var arg : method.args()) {
      env = env.augment(arg.name().id(), new Type.Basic(arg.type().print(0)));
    }
    for (final var variables : method.vars()) {
      env = env.augment(variables.name().id(), new Type.Basic(variables.type().print(0)));
    }
    final var astArgs =
        method.args().stream().map(jlitec.ast.Var::new).collect(Collectors.toUnmodifiableList());
    final var astVars =
        method.vars().stream().map(jlitec.ast.Var::new).collect(Collectors.toUnmodifiableList());
    final var expectedReturnType = new Type.Basic(method.type().print(0));
    final var astStmtList = toAst(method.stmtList(), expectedReturnType, klassDescriptorMap, env);
    return new jlitec.ast.Method(
        jlitec.ast.Type.fromParseTree(method.type()),
        method.name().id(),
        astArgs,
        astVars,
        astStmtList);
  }

  private static List<jlitec.ast.stmt.Stmt> toAst(
      List<Stmt> stmtList,
      Type.Basic expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env) {
    return stmtList.stream()
        .map(
            stmt -> {
              try {
                return toAst(stmt, expectedReturnType, klassDescriptorMap, env);
              } catch (SemanticException e) {
                // should not throw, typecheck has already been performed.
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toUnmodifiableList());
  }

  private static jlitec.ast.stmt.Stmt toAst(
      Stmt stmt,
      Type.Basic expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    final var type = typecheck(stmt, expectedReturnType, klassDescriptorMap, env);
    final var typeAnnotation = toAst(type);
    return switch (stmt.getStmtType()) {
      case STMT_IF -> {
        final var is = (IfStmt) stmt;
        final var condition = toAst(is.condition(), klassDescriptorMap, env);
        final var thenStmtList = new ArrayList<jlitec.ast.stmt.Stmt>();
        for (final var innerStmt : is.thenStmtList()) {
          thenStmtList.add(toAst(innerStmt, expectedReturnType, klassDescriptorMap, env));
        }
        final var elseStmtList = new ArrayList<jlitec.ast.stmt.Stmt>();
        for (final var innerStmt : is.elseStmtList()) {
          elseStmtList.add(toAst(innerStmt, expectedReturnType, klassDescriptorMap, env));
        }
        yield new jlitec.ast.stmt.IfStmt(condition, thenStmtList, elseStmtList);
      }
      case STMT_WHILE -> {
        final var ws = (WhileStmt) stmt;
        final var condition = toAst(ws.condition(), klassDescriptorMap, env);
        final var stmtList = new ArrayList<jlitec.ast.stmt.Stmt>();
        for (final var innerStmt : ws.stmtList()) {
          stmtList.add(toAst(innerStmt, expectedReturnType, klassDescriptorMap, env));
        }
        yield new jlitec.ast.stmt.WhileStmt(condition, stmtList);
      }
      case STMT_READLN -> new jlitec.ast.stmt.ReadlnStmt(((ReadlnStmt) stmt).id());
      case STMT_PRINTLN -> new jlitec.ast.stmt.PrintlnStmt(
          toAst(((PrintlnStmt) stmt).expr(), klassDescriptorMap, env));
      case STMT_VAR_ASSIGN -> {
        final var vas = (VarAssignStmt) stmt;
        final var rhs = toAst(vas.rhs(), klassDescriptorMap, env);
        yield new jlitec.ast.stmt.VarAssignStmt(vas.lhsId(), rhs);
      }
      case STMT_FIELD_ASSIGN -> {
        final var fas = (FieldAssignStmt) stmt;
        final var lhsTarget = toAst(fas.lhsTarget(), klassDescriptorMap, env);
        final var rhs = toAst(fas.rhs(), klassDescriptorMap, env);
        yield new jlitec.ast.stmt.FieldAssignStmt(lhsTarget, fas.lhsId(), rhs);
      }
      case STMT_CALL -> {
        final var cs = (CallStmt) stmt;
        final var args = new ArrayList<jlitec.ast.expr.Expr>();
        for (final var arg : cs.args()) {
          args.add(toAst(arg, klassDescriptorMap, env));
        }
        final var argTypes = new ArrayList<String>();
        for (final var arg : cs.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env).type());
        }
        final var target =
            transformCallTarget(cs.target(), typeAnnotation, klassDescriptorMap, env);
        final var methodReference =
            lookupMethodReference(cs.target(), argTypes, klassDescriptorMap, env);
        yield new jlitec.ast.stmt.CallStmt(target, args, typeAnnotation, methodReference);
      }
      case STMT_RETURN -> {
        final var rs = (ReturnStmt) stmt;
        final var maybeExpr = rs.maybeExpr();
        if (maybeExpr.isPresent()) {
          yield new jlitec.ast.stmt.ReturnStmt(
              Optional.of(toAst(maybeExpr.get(), klassDescriptorMap, env)),
              toAst(expectedReturnType));
        } else {
          yield new jlitec.ast.stmt.ReturnStmt(Optional.empty(), toAst(expectedReturnType));
        }
      }
    };
  }

  private static jlitec.ast.expr.Expr transformCallTarget(
      Expr target,
      TypeAnnotation typeAnnotation,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env) {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_NEW, EXPR_NULL, EXPR_CALL -> throw new RuntimeException(
          "Trying to call non-callable expression.");
      case EXPR_ID -> {
        // local call
        final var ie = (IdExpr) target;
        yield new jlitec.ast.expr.IdExpr(ie.id(), typeAnnotation);
      }
      case EXPR_DOT -> {
        // global call
        try {
          final var de = (DotExpr) target;
          final var targetType = typecheck(de.target(), klassDescriptorMap, env);
          final var newTarget =
              switch (de.target().getExprType()) {
                case EXPR_NEW -> {
                  final var ne = (NewExpr) de.target();
                  yield new jlitec.ast.expr.NewExpr(ne.cname());
                }
                case EXPR_THIS -> {
                  final var te = (jlitec.parsetree.expr.ThisExpr) de.target();
                  yield new ThisExpr(new TypeAnnotation.Klass(env.lookup("this").get().type()));
                }
                case EXPR_CALL -> {
                  try {
                    yield toAst(target, klassDescriptorMap, env);
                  } catch (SemanticException e) {
                    throw new RuntimeException(e);
                  }
                }
                case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_DOT, EXPR_ID, EXPR_NULL, EXPR_PAREN -> {
                  yield transformCallTarget(de.target(), typeAnnotation, klassDescriptorMap, env);
                }
              };
          yield new jlitec.ast.expr.DotExpr(newTarget, de.id(), toAst(targetType));
        } catch (SemanticException e) {
          throw new RuntimeException(e);
        }
      }
      case EXPR_PAREN -> {
        final var pe = (ParenExpr) target;
        yield transformCallTarget(pe.expr(), typeAnnotation, klassDescriptorMap, env);
      }
    };
  }

  private static MethodReference lookupMethodReference(
      Expr target,
      List<String> argTypes,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env) {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> {
        // should not throw, checks have already been performed
        throw new RuntimeException("Trying to call non-callable expression.");
      }
        // LocalCall
      case EXPR_ID -> {
        final var ie = (IdExpr) target;
        // this, and the method descriptor must exist in the env
        final var cname = env.lookup("this").get().type();
        final var klassDescriptor = klassDescriptorMap.get(env.lookup("this").get().type());
        final var methodDescriptors = klassDescriptor.methods().get(ie.id());
        final var methodDescriptor =
            methodDescriptors.stream()
                .filter(
                    m ->
                        isCompatible(
                            argTypes,
                            m.argTypes().stream()
                                .map(Type.Basic::type)
                                .collect(Collectors.toUnmodifiableList())))
                .findFirst()
                .get();
        final var returnType = jlitec.ast.Type.fromChecker(methodDescriptor.returnType());
        final var astArgTypes =
            methodDescriptor.argTypes().stream()
                .map(jlitec.ast.Type::fromChecker)
                .collect(Collectors.toUnmodifiableList());
        yield new MethodReference(cname, ie.id(), returnType, astArgTypes);
      }
      case EXPR_DOT -> {
        try {
          final var de = (DotExpr) target;
          final var targetType = typecheck(de.target(), klassDescriptorMap, env);
          final var klassDescriptor = klassDescriptorMap.get(targetType.type());
          final var methodDescriptors = klassDescriptor.methods().get(de.id());
          final var methodDescriptor =
              methodDescriptors.stream()
                  .filter(
                      m ->
                          isCompatible(
                              argTypes,
                              m.argTypes().stream()
                                  .map(Type.Basic::type)
                                  .collect(Collectors.toUnmodifiableList())))
                  .findFirst()
                  .get();
          final var returnType = jlitec.ast.Type.fromChecker(methodDescriptor.returnType());
          final var astArgTypes =
              methodDescriptor.argTypes().stream()
                  .map(jlitec.ast.Type::fromChecker)
                  .collect(Collectors.toUnmodifiableList());
          yield new MethodReference(targetType.type(), de.id(), returnType, astArgTypes);
        } catch (SemanticException e) {
          // should not throw, checks have already been performed
          throw new RuntimeException(e);
        }
      }
      case EXPR_PAREN -> lookupMethodReference(
          ((ParenExpr) target).expr(), argTypes, klassDescriptorMap, env);
    };
  }

  private static jlitec.ast.expr.Expr toAst(
      Expr expr, Map<String, KlassDescriptor> klassDescriptorMap, Environment env)
      throws SemanticException {
    final var type = typecheck(expr, klassDescriptorMap, env);
    final var typeAnnotation = toAst(type);
    return switch (expr.getExprType()) {
      case EXPR_INT_LITERAL -> new jlitec.ast.expr.IntLiteralExpr(((IntLiteralExpr) expr).value());
      case EXPR_STRING_LITERAL -> new jlitec.ast.expr.StringLiteralExpr(
          ((StringLiteralExpr) expr).value());
      case EXPR_BOOL_LITERAL -> new jlitec.ast.expr.BoolLiteralExpr(
          ((BoolLiteralExpr) expr).value());
      case EXPR_BINARY -> {
        final var be = (BinaryExpr) expr;
        final var lhs = toAst(be.lhs(), klassDescriptorMap, env);
        final var rhs = toAst(be.rhs(), klassDescriptorMap, env);
        yield new jlitec.ast.expr.BinaryExpr(
            BinaryOp.fromParseTree(be.op()), lhs, rhs, typeAnnotation);
      }
      case EXPR_UNARY -> {
        final var ue = (UnaryExpr) expr;
        final var astExpr = toAst(ue.expr(), klassDescriptorMap, env);
        yield new jlitec.ast.expr.UnaryExpr(
            UnaryOp.fromParseTree(ue.op()), astExpr, typeAnnotation);
      }
      case EXPR_DOT -> {
        final var de = (DotExpr) expr;
        final var target = toAst(de.target(), klassDescriptorMap, env);
        yield new jlitec.ast.expr.DotExpr(target, de.id(), typeAnnotation);
      }
      case EXPR_CALL -> {
        final var ce = (CallExpr) expr;
        final var target =
            transformCallTarget(ce.target(), typeAnnotation, klassDescriptorMap, env);
        final var args = new ArrayList<jlitec.ast.expr.Expr>();
        for (final var arg : ce.args()) {
          args.add(toAst(arg, klassDescriptorMap, env));
        }
        final var argTypes = new ArrayList<String>();
        for (final var arg : ce.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env).type());
        }
        final var methodReference =
            lookupMethodReference(ce.target(), argTypes, klassDescriptorMap, env);
        yield new jlitec.ast.expr.CallExpr(target, args, typeAnnotation, methodReference);
      }
      case EXPR_THIS -> new ThisExpr(typeAnnotation);
      case EXPR_ID -> new jlitec.ast.expr.IdExpr(((IdExpr) expr).id(), typeAnnotation);
      case EXPR_NEW -> new jlitec.ast.expr.NewExpr(((NewExpr) expr).cname());
      case EXPR_NULL -> new jlitec.ast.expr.NullExpr();
      case EXPR_PAREN -> toAst(((ParenExpr) expr).expr(), klassDescriptorMap, env);
    };
  }

  private static TypeAnnotation toAst(Type.Basic basicType) {
    final var type = basicType.type();
    if (type.equals(JliteType.STRING.print(0))) {
      return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.STRING);
    } else if (type.equals(JliteType.INT.print(0))) {
      return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.INT);
    } else if (type.equals(JliteType.BOOL.print(0))) {
      return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.BOOL);
    } else if (type.equals(JliteType.VOID.print(0))) {
      return new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID);
    } else if (basicType.equals(Type.Basic.NULL)) {
      return new TypeAnnotation.Null();
    } else {
      return new TypeAnnotation.Klass(type);
    }
  }

  public static void typecheck(Program program, Map<String, KlassDescriptor> klassDescriptorMap)
      throws SemanticException {
    final Environment env = new Environment();
    for (final var klass : program.klassList()) {
      typecheck(klass, klassDescriptorMap, env);
    }
  }

  private static void typecheck(
      Klass klass, Map<String, KlassDescriptor> klassDescriptorMap, Environment env)
      throws SemanticException {
    final var klassName = klass.name().id();
    env = env.augment(new Environment(klassDescriptorMap.get(klassName)));
    env = env.augment("this", new Type.Basic(klassName));
    for (final var method : klass.methods()) {
      typecheck(method, klassDescriptorMap, env);
    }
  }

  private static void typecheck(
      Method method, Map<String, KlassDescriptor> klassDescriptorMap, Environment env)
      throws SemanticException {
    for (final var arg : method.args()) {
      env = env.augment(arg.name().id(), new Type.Basic(arg.type().print(0)));
    }
    for (final var variables : method.vars()) {
      env = env.augment(variables.name().id(), new Type.Basic(variables.type().print(0)));
    }
    // check statements
    final var expectedReturnType = new Type.Basic(method.type().print(0));
    final var returnType =
        typecheck(method.stmtList(), expectedReturnType, klassDescriptorMap, env);
    final var signature =
        method.name().id()
            + "("
            + method.args().stream().map(v -> v.type().print(0)).collect(Collectors.joining(", "))
            + ")";
    if (!isCompatible(returnType, expectedReturnType)) {
      throw new SemanticException(
          "Incompatible return type for method `"
              + signature
              + "'. Expected `"
              + expectedReturnType.type()
              + "' but encountered `"
              + returnType.type()
              + "'",
          "Incompatible return type",
          List.of(method.type()));
    }
  }

  private static Type.Basic typecheck(
      List<Stmt> stmts,
      Type.Basic expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    Type.Basic type = new Type.Basic(JliteType.VOID);
    for (final var stmt : stmts) {
      type = typecheck(stmt, expectedReturnType, klassDescriptorMap, env);
    }
    return type;
  }

  private static Type.Basic typecheck(
      Stmt stmt,
      Type.Basic expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    return switch (stmt.getStmtType()) {
      case STMT_VAR_ASSIGN -> {
        final var vas = (VarAssignStmt) stmt;
        final var maybeType = env.lookup(vas.lhsId());
        if (maybeType.isEmpty()) {
          throw new SemanticException(
              "Use of undeclared identifier `" + vas.lhsId() + "'",
              "undeclared identifier `" + vas.lhsId() + "'",
              List.of(vas));
        }
        final var expectedType = maybeType.get();
        final var rhsType = typecheck(vas.rhs(), klassDescriptorMap, env);
        if (!isCompatible(expectedType, rhsType)) {
          throw new SemanticException(
              "Incompatible types, trying to assign expression of type `"
                  + rhsType.type()
                  + "' to variable `"
                  + vas.lhsId()
                  + "' of type `"
                  + expectedType.type()
                  + "'",
              "incompatible types",
              List.of(vas));
        }
        yield expectedType;
      }
      case STMT_FIELD_ASSIGN -> {
        final var fas = (FieldAssignStmt) stmt;
        final var lhsClassType = typecheck(fas.lhsTarget(), klassDescriptorMap, env);
        final var klass = klassDescriptorMap.get(lhsClassType.type());
        final var maybeFieldType = Optional.ofNullable(klass.fields().get(fas.lhsId()));
        if (maybeFieldType.isEmpty()) {
          throw new SemanticException(
              "Field `" + fas.lhsId() + "' does not exist in class `" + lhsClassType.type() + "'",
              "non-existent field `" + fas.lhsId() + "'",
              List.of(fas));
        }
        final var expectedType = maybeFieldType.get();
        final var rhsType = typecheck(fas.rhs(), klassDescriptorMap, env);
        if (!isCompatible(expectedType, rhsType)) {
          throw new SemanticException(
              "Incompatible types, trying to assign expression of type `"
                  + rhsType.type()
                  + "' to field `"
                  + fas.lhsId()
                  + "' of class `"
                  + lhsClassType.type()
                  + "' of type `"
                  + expectedType.type()
                  + "'",
              "incompatible types",
              List.of(fas));
        }
        yield expectedType;
      }
      case STMT_IF -> {
        final var is = (IfStmt) stmt;
        final var conditionType = typecheck(is.condition(), klassDescriptorMap, env);
        if (!conditionType.equals(new Type.Basic(JliteType.BOOL))) {
          throw new SemanticException(
              "If condition expression must be of type `Bool', but encountered `"
                  + conditionType.type()
                  + "'",
              "condition type is not `Bool'",
              List.of(is.condition()));
        }
        final var thenType =
            typecheck(is.thenStmtList(), expectedReturnType, klassDescriptorMap, env);
        final var elseType =
            typecheck(is.elseStmtList(), expectedReturnType, klassDescriptorMap, env);
        if (!isCompatible(thenType, elseType)) {
          throw new SemanticException(
              "The types of then and else blocks of conditionals must be compatible, then block type is `"
                  + thenType.type()
                  + "', else block type is `"
                  + elseType.type()
                  + "'",
              "incompatible then and else type",
              List.of(is));
        }
        yield elseType;
      }
      case STMT_WHILE -> {
        final var ws = (WhileStmt) stmt;
        final var conditionType = typecheck(ws.condition(), klassDescriptorMap, env);
        if (!conditionType.equals(new Type.Basic(JliteType.BOOL))) {
          throw new SemanticException(
              "While condition expression must be of type `Bool', but encountered `"
                  + conditionType.type()
                  + "'",
              "condition type is not `Bool'",
              List.of(ws.condition()));
        }
        if (ws.stmtList().isEmpty()) {
          yield new Type.Basic(JliteType.VOID);
        }
        yield typecheck(ws.stmtList(), expectedReturnType, klassDescriptorMap, env);
      }
      case STMT_READLN -> {
        final var rs = (ReadlnStmt) stmt;
        final var maybeVarType = env.lookup(rs.id());
        if (maybeVarType.isEmpty()) {
          throw new SemanticException(
              "Use of undeclared identifier `" + rs.id() + "'",
              "undeclared identifier `" + rs.id() + "'",
              List.of(rs));
        }
        final var varType = maybeVarType.get();
        if (!Set.of(JliteType.INT.print(0), JliteType.BOOL.print(0), JliteType.STRING.print(0))
            .contains(varType.type())) {
          throw new SemanticException(
              "Type of variable passed to `readln' must be `Int', `Bool', or `String', but encountered `"
                  + varType.type()
                  + "'",
              "incompatible type",
              List.of(rs));
        }
        yield new Type.Basic(JliteType.VOID);
      }
      case STMT_PRINTLN -> {
        final var ps = (PrintlnStmt) stmt;
        final var psType = typecheck(ps.expr(), klassDescriptorMap, env);
        if (!Set.of(JliteType.INT.print(0), JliteType.BOOL.print(0), JliteType.STRING.print(0))
            .contains(psType.type())) {
          throw new SemanticException(
              "Type of expression passed to `println' must be `Int', `Bool', or `String', but encountered `"
                  + psType.type()
                  + "'",
              "incompatible type",
              List.of(ps));
        }
        yield new Type.Basic(JliteType.VOID);
      }
      case STMT_CALL -> {
        final var cs = (CallStmt) stmt;
        final var argTypes = new ArrayList<String>();
        for (final var arg : cs.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env).type());
        }
        yield lookupMethodReturnType(cs.target(), argTypes, klassDescriptorMap, env);
      }
      case STMT_RETURN -> {
        final var rs = (ReturnStmt) stmt;
        final var maybeExpr = rs.maybeExpr();
        if (maybeExpr.isEmpty()) {
          if (!expectedReturnType.equals(new Type.Basic(JliteType.VOID))) {
            throw new SemanticException(
                "Returning `Void' for a method that expects return type `"
                    + expectedReturnType.type()
                    + "'",
                "invalid return type",
                List.of(rs));
          }
          yield new Type.Basic(JliteType.VOID);
        }
        final var expr = maybeExpr.get();
        final var exprType = typecheck(expr, klassDescriptorMap, env);
        if (!isCompatible(expectedReturnType, exprType)) {
          throw new SemanticException(
              "Returning `"
                  + exprType.type()
                  + "' for a method that expects return type `"
                  + expectedReturnType.type()
                  + "'",
              "invalid return type",
              List.of(rs));
        }
        yield exprType;
      }
    };
  }

  private static Type.Basic typecheck(
      Expr expr, Map<String, KlassDescriptor> klassDescriptorMap, Environment env)
      throws SemanticException {
    return switch (expr.getExprType()) {
      case EXPR_PAREN -> {
        final var ep = (ParenExpr) expr;
        yield typecheck(ep.expr(), klassDescriptorMap, env);
      }
      case EXPR_THIS -> env.lookup("this").get();
      case EXPR_ID -> {
        final var ie = (IdExpr) expr;
        final var maybeType = env.lookup(ie.id());
        if (maybeType.isEmpty()) {
          throw new SemanticException(
              "Use of undeclared identifier `" + ie.id() + "'",
              "undeclared identifier `" + ie.id() + "'",
              List.of(ie));
        }
        yield maybeType.get();
      }
      case EXPR_INT_LITERAL -> new Type.Basic(JliteType.INT);
      case EXPR_BOOL_LITERAL -> new Type.Basic(JliteType.BOOL);
      case EXPR_STRING_LITERAL -> new Type.Basic(JliteType.STRING);
      case EXPR_DOT -> {
        final var de = (DotExpr) expr;
        final var targetType = typecheck(de.target(), klassDescriptorMap, env);
        final var klassDescriptor = klassDescriptorMap.get(targetType.type());
        final var maybeFieldType =
            Optional.ofNullable(klassDescriptor.fields().getOrDefault(de.id(), null));
        if (maybeFieldType.isEmpty()) {
          throw new SemanticException(
              "Field `" + de.id() + "' does not exist in class `" + targetType.type() + "'",
              "non-existent field `" + de.id() + "'",
              List.of(de));
        }
        yield maybeFieldType.get();
      }
      case EXPR_NEW -> {
        final var ne = (NewExpr) expr;
        if (!klassDescriptorMap.containsKey(ne.cname())) {
          throw new SemanticException(
              "Trying to instantiate a non-existent class `" + ne.cname() + "'",
              "instantiation of non-existent class `" + ne.cname() + "'",
              List.of(ne));
        }
        yield new Type.Basic(((NewExpr) expr).cname());
      }
      case EXPR_CALL -> {
        final var ce = (CallExpr) expr;
        final var argTypes = new ArrayList<String>();
        for (final var arg : ce.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env).type());
        }
        yield lookupMethodReturnType(ce.target(), argTypes, klassDescriptorMap, env);
      }
      case EXPR_BINARY -> {
        final var be = (BinaryExpr) expr;
        final var lhsType = typecheck(be.lhs(), klassDescriptorMap, env);
        final var rhsType = typecheck(be.rhs(), klassDescriptorMap, env);
        yield switch (be.op()) {
          case PLUS -> {
            if (lhsType.equals(new Type.Basic(JliteType.INT))) {
              if (!rhsType.equals(new Type.Basic(JliteType.INT))) {
                throw new SemanticException(
                    "Arithmetic binary operator `+' second operand must be `Int' but encountered `"
                        + rhsType.type()
                        + "'",
                    "wrong operand type for arithmetic binary operator",
                    List.of(be.rhs()));
              }
              yield new Type.Basic(JliteType.INT);
            } else if (lhsType.equals(new Type.Basic(JliteType.STRING))
                || lhsType.equals(Type.Basic.NULL)) {
              if (rhsType.equals(new Type.Basic(JliteType.STRING))
                  || rhsType.equals(Type.Basic.NULL)) {
                yield new Type.Basic(JliteType.STRING);
              } else {
                throw new SemanticException(
                    "String concatenation operator `+' second operand must be `String' or `null' but encountered `"
                        + rhsType.type()
                        + "'",
                    "wrong operand type for string concatenation",
                    List.of(be.rhs()));
              }
            } else {
              throw new SemanticException(
                  "Invalid first operand type for binary operator `+', expected either `Int', `String', or `null' but encountered `"
                      + lhsType.type()
                      + "'",
                  "wrong operand type for `+'",
                  List.of(be));
            }
          }
          case MINUS, MULT, DIV -> {
            if (!lhsType.equals(new Type.Basic(JliteType.INT))) {
              throw new SemanticException(
                  "Arithmetic binary operator `"
                      + be.op().toString()
                      + "' first operand must be `Int' but encountered `"
                      + lhsType.type()
                      + "'",
                  "wrong operand type for arithmetic binary operator",
                  List.of(be.lhs()));
            }
            if (!rhsType.equals(new Type.Basic(JliteType.INT))) {
              throw new SemanticException(
                  "Arithmetic binary operator `"
                      + be.op().toString()
                      + "' second operand must be `Int' but encountered `"
                      + rhsType.type()
                      + "'",
                  "wrong operand type for arithmetic binary operator",
                  List.of(be.rhs()));
            }
            yield new Type.Basic(JliteType.INT);
          }
          case LT, GT, LEQ, GEQ, EQ, NEQ -> {
            if (!lhsType.equals(new Type.Basic(JliteType.INT))) {
              throw new SemanticException(
                  "Comparison binary operator `"
                      + be.op().toString()
                      + "' first operand must be `Int' but encountered `"
                      + lhsType.type()
                      + "'",
                  "wrong operand type for comparison binary operator",
                  List.of(be.lhs()));
            }
            if (!rhsType.equals(new Type.Basic(JliteType.INT))) {
              throw new SemanticException(
                  "Comparison binary operator `"
                      + be.op().toString()
                      + "' second operand must be `Int' but encountered `"
                      + rhsType.type()
                      + "'",
                  "wrong operand type for comparison binary operator",
                  List.of(be.rhs()));
            }
            yield new Type.Basic(JliteType.BOOL);
          }
          case AND, OR -> {
            if (!lhsType.equals(new Type.Basic(JliteType.BOOL))) {
              throw new SemanticException(
                  "Boolean binary operator `"
                      + be.op().toString()
                      + "' first operand must be `Bool' but encountered `"
                      + lhsType.type()
                      + "'",
                  "wrong operand type for boolean binary operator",
                  List.of(be.lhs()));
            }
            if (!rhsType.equals(new Type.Basic(JliteType.BOOL))) {
              throw new SemanticException(
                  "Boolean binary operator `"
                      + be.op().toString()
                      + "' second operand must be `Bool' but encountered `"
                      + rhsType.type()
                      + "'",
                  "wrong operand type for boolean binary operator",
                  List.of(be.rhs()));
            }
            yield new Type.Basic(JliteType.BOOL);
          }
        };
      }
      case EXPR_UNARY -> {
        final var ue = (UnaryExpr) expr;
        final var exprType = typecheck(ue.expr(), klassDescriptorMap, env);
        yield switch (ue.op()) {
          case NEGATIVE -> {
            if (!exprType.equals(new Type.Basic(JliteType.INT))) {
              throw new SemanticException(
                  "Negation can only be applied to `Int` type, but encountered `"
                      + exprType.type()
                      + "'",
                  "invalid type for negation",
                  List.of(ue));
            }
            yield new Type.Basic(JliteType.INT);
          }
          case NOT -> {
            if (!exprType.equals(new Type.Basic(JliteType.BOOL))) {
              throw new SemanticException(
                  "Complement can only be applied to `Bool` type, but encountered `"
                      + exprType.type()
                      + "'",
                  "invalid type for negation",
                  List.of(ue));
            }
            yield new Type.Basic(JliteType.BOOL);
          }
        };
      }
      case EXPR_NULL -> Type.Basic.NULL;
    };
  }

  private static Type.Basic lookupMethodReturnType(
      Expr target,
      List<String> argTypes,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> throw new SemanticException(
          "Trying to call non-callable expression.", "non-callable expression", List.of(target));
        // LocalCall
      case EXPR_ID -> {
        final var ie = (IdExpr) target;
        final var klassDescriptor = klassDescriptorMap.get(env.lookup("this").get().type());
        final var maybeMethods = Optional.ofNullable(klassDescriptor.methods().get(ie.id()));
        if (maybeMethods.isEmpty()) {
          final var signature = ie.id() + "(" + String.join(", ", argTypes) + ")";
          throw new SemanticException(
              "Local method `" + signature + "' does not exist.",
              "non-existent local method `" + signature + "'",
              List.of(ie));
        }
        final var returnTypes =
            maybeMethods.get().stream()
                .filter(
                    m ->
                        isCompatible(
                            argTypes,
                            m.argTypes().stream()
                                .map(Type.Basic::type)
                                .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toUnmodifiableList());
        if (returnTypes.isEmpty()) {
          final var signature = ie.id() + "(" + String.join(", ", argTypes) + ")";
          throw new SemanticException(
              "Local method `" + signature + "' does not exist.",
              "non-existent method `" + signature + "'",
              List.of(ie));
        }
        if (returnTypes.size() > 1) {
          final var signature = ie.id() + "(" + String.join(", ", argTypes) + ")";
          final var possibleMethods =
              returnTypes.stream()
                  .map(
                      m ->
                          ie.id()
                              + "("
                              + m.argTypes().stream()
                                  .map(Type.Basic::type)
                                  .collect(Collectors.joining(", "))
                              + ")")
                  .collect(Collectors.joining(", "));
          throw new SemanticException(
              "Local call with signature `"
                  + signature
                  + "' is ambiguous. Possible method overloads: "
                  + possibleMethods,
              "ambiguous method call `" + signature + "'",
              List.of(ie));
        }
        yield returnTypes.get(0).returnType();
      }
        // GlobalCall
      case EXPR_DOT -> {
        final var de = (DotExpr) target;
        final var targetType = typecheck(de.target(), klassDescriptorMap, env);
        if (targetType.equals(Type.Basic.NULL)) {
          throw new SemanticException(
              "Trying to access a method of `null'", "calling a method on `null'", List.of(target));
        }
        final var klassDescriptor = klassDescriptorMap.get(targetType.type());
        final var maybeMethods = Optional.ofNullable(klassDescriptor.methods().get(de.id()));
        if (maybeMethods.isEmpty()) {
          final var signature = de.id() + "(" + String.join(", ", argTypes) + ")";
          throw new SemanticException(
              "Method `" + signature + "' is not found on class `" + targetType.type() + "'",
              "non-existent method `" + signature + "'",
              List.of(de));
        }
        final var returnTypes =
            maybeMethods.get().stream()
                .filter(
                    m ->
                        isCompatible(
                            argTypes,
                            m.argTypes().stream()
                                .map(Type.Basic::type)
                                .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toUnmodifiableList());
        if (returnTypes.isEmpty()) {
          final var signature = de.id() + "(" + String.join(", ", argTypes) + ")";
          throw new SemanticException(
              "Method `" + signature + "' is not found on class `" + targetType.type() + "'",
              "non-existent method `" + signature + "'",
              List.of(de));
        }
        if (returnTypes.size() > 1) {
          final var signature = de.id() + "(" + String.join(", ", argTypes) + ")";
          final var possibleMethods =
              returnTypes.stream()
                  .map(
                      m ->
                          de.id()
                              + "("
                              + m.argTypes().stream()
                                  .map(Type.Basic::type)
                                  .collect(Collectors.joining(", "))
                              + ")")
                  .collect(Collectors.joining(", "));
          throw new SemanticException(
              "Call with signature `"
                  + signature
                  + "' on class `"
                  + targetType.type()
                  + "' is ambiguous. Possible method overloads: "
                  + possibleMethods,
              "ambiguous method call `" + signature + "'",
              List.of(de));
        }
        yield returnTypes.get(0).returnType();
      }
      case EXPR_PAREN -> lookupMethodReturnType(
          ((ParenExpr) target).expr(), argTypes, klassDescriptorMap, env);
    };
  }

  private static boolean isCompatible(List<String> actualTypes, List<String> expectedTypes) {
    if (actualTypes.size() != expectedTypes.size()) return false;
    final var primitiveType = Set.of(JliteType.BOOL.print(0), JliteType.INT.print(0));
    for (int i = 0; i < actualTypes.size(); i++) {
      final var actualType = actualTypes.get(i);
      final var expectedType = expectedTypes.get(i);

      if (primitiveType.contains(expectedType)) {
        if (!expectedType.equals(actualType)) return false;
      } else if (expectedType.equals(JliteType.STRING.print(0))) {
        if (!Set.of(JliteType.STRING.print(0), Type.Basic.NULL.type()).contains(actualType))
          return false;
      } else {
        // Must be non-string, non-primitive, class type
        if (!actualType.equals(Type.Basic.NULL.type()) && !actualType.equals(expectedType))
          return false;
      }
    }
    return true;
  }

  private static boolean isCompatible(Type.Basic type1, Type.Basic type2) {
    if (type1.equals(type2)) return true;
    final var primitiveType = Set.of(JliteType.BOOL.print(0), JliteType.INT.print(0));
    // if types are not equal
    if (primitiveType.contains(type1.type())) {
      // primitive types must be equal
      return false;
    } else if (type1.type().equals(JliteType.STRING.print(0))) {
      final var stringLikeType = Set.of(JliteType.STRING.print(0), Type.Basic.NULL.type());
      return stringLikeType.contains(type2.type());
    } else if (type1.equals(Type.Basic.NULL)) {
      return !primitiveType.contains(type2.type());
    } else {
      // must be non-string, non-primitive, class type
      return type2.equals(Type.Basic.NULL);
    }
  }

  public static void distinctNameCheck(Program program) throws SemanticException {
    distinctClassNameCheck(program);
    for (final var klass : program.klassList()) {
      distinctFieldNameCheck(klass);
      distinctMethodNameCheck(klass);
      distinctParamNameCheck(klass);
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

  private static void distinctParamNameCheck(Klass klass) throws SemanticException {
    for (final var method : klass.methods()) {
      final var names = method.args().stream().collect(Collectors.groupingBy(m -> m.name().id()));
      for (final var entry : names.entrySet()) {
        final var paramList = entry.getValue();
        if (paramList.size() > 1) {
          throw new SemanticException(
              "Names of parameters in a method signature must be distinct: parameter `"
                  + paramList.get(0).name().id()
                  + "'",
              "duplicate parameter name",
              paramList);
        }
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
