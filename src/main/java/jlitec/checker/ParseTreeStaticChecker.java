package jlitec.checker;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.ast.MethodReference;
import jlitec.ast.TypeAnnotation;
import jlitec.ast.expr.BinaryOp;
import jlitec.ast.expr.ThisExpr;
import jlitec.ast.expr.UnaryOp;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Klass;
import jlitec.parsetree.KlassType;
import jlitec.parsetree.Locatable;
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
  // prevent instantiation
  private ParseTreeStaticChecker() {}

  public static String generateErrorMessage(SemanticException e, ParserWrapper parser) {
    final var sb = new StringBuilder();
    final var lines =
        e.locatableList.stream()
            .map(Locatable::leftLocation)
            .map(
                l -> "--> " + parser.filename + ":" + (l.getLine() + 1) + ":" + (l.getColumn() + 1))
            .collect(Collectors.joining("\n"));
    sb.append(lines).append("\nSemantic error: ").append(e.getMessage()).append("\n");
    for (final var locatable : e.locatableList) {
      sb.append(parser.formErrorString(e.shortMessage, locatable)).append("\n");
    }
    return sb.toString();
  }

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
                                        v -> v.name().id(), v -> Type.fromParseTree(v.type()))),
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
                                                            Type.fromParseTree(m.type()),
                                                            m.args().stream()
                                                                .map(
                                                                    v ->
                                                                        Type.fromParseTree(
                                                                            v.type()))
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
              "Non-existent class type `" + type.print(0) + "'",
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
              "Non-existent class type `" + returnType.print(0) + "'",
              "non-existent class type",
              List.of(returnType));
        }
        // check args
        for (final var arg : method.args()) {
          final var type = arg.type();
          if (type instanceof KlassType klassType
              && !klassDescriptorMap.containsKey(klassType.cname())) {
            throw new SemanticException(
                "Non-existent class type `" + type.print(0) + "'",
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
                "Non-existent class type `" + type.print(0) + "'",
                "non-existent class type",
                List.of(type));
          }
        }
      }
    }
  }

  public static jlitec.ast.Program toAst(
      jlitec.parsetree.Program program, Map<String, KlassDescriptor> klassDescriptorMap)
      throws SemanticException {
    final Environment env = new Environment();
    final var astKlasses = new ArrayList<jlitec.ast.Klass>();
    for (final var klass : program.klassList()) {
      astKlasses.add(toAst(klass, klassDescriptorMap, env));
    }
    return new jlitec.ast.Program(astKlasses);
  }

  private static jlitec.ast.Klass toAst(
      jlitec.parsetree.Klass klass,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    final var klassName = klass.name().id();
    env = env.augment(new Environment(klassDescriptorMap.get(klassName)));
    env = env.augment("this", new Type.Klass(klassName));

    final var astFields =
        klass.fields().stream().map(jlitec.ast.Var::new).collect(Collectors.toUnmodifiableList());

    final var astMethods = new ArrayList<jlitec.ast.Method>();
    for (final var method : klass.methods()) {
      astMethods.add(toAst(method, klassDescriptorMap, env));
    }
    return new jlitec.ast.Klass(klassName, astFields, astMethods);
  }

  private static jlitec.ast.Method toAst(
      jlitec.parsetree.Method method,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    for (final var arg : method.args()) {
      env = env.augment(arg.name().id(), Type.fromParseTree(arg.type()));
    }
    for (final var variables : method.vars()) {
      env = env.augment(variables.name().id(), Type.fromParseTree(variables.type()));
    }

    final var astArgs =
        method.args().stream().map(jlitec.ast.Var::new).collect(Collectors.toUnmodifiableList());
    final var astVars =
        method.vars().stream().map(jlitec.ast.Var::new).collect(Collectors.toUnmodifiableList());

    final var expectedReturnType = Type.fromParseTree(method.type());
    final var astStmtList = toAst(method.stmtList(), expectedReturnType, klassDescriptorMap, env);

    // check statements
    final var returnTypeAnnotation =
        Optional.ofNullable(Iterables.getLast(astStmtList, null))
            .map(jlitec.ast.stmt.Stmt::typeAnnotation)
            .orElse(new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID));
    final var returnType = fromTypeAnnotation(returnTypeAnnotation);

    if (!isCompatible(returnType, expectedReturnType)) {
      final var signature =
          method.name().id()
              + "("
              + method.args().stream().map(v -> v.type().print(0)).collect(Collectors.joining(", "))
              + ")";
      throw new SemanticException(
          "Incompatible return type for method `"
              + signature
              + "'. Expected `"
              + expectedReturnType.friendlyName()
              + "' but encountered `"
              + returnType.friendlyName()
              + "'",
          "Incompatible return type",
          List.of(method.type()));
    }

    return new jlitec.ast.Method(
        jlitec.ast.Type.fromParseTree(method.type()),
        method.name().id(),
        astArgs,
        astVars,
        astStmtList);
  }

  private static Type fromTypeAnnotation(TypeAnnotation typeAnnotation) {
    return switch (typeAnnotation.annotation()) {
      case NULL -> Type.NULL;
      case INT -> Type.INT;
      case VOID -> Type.VOID;
      case STRING -> Type.STRING;
      case BOOL -> Type.BOOL;
      case CLASS -> new Type.Klass(((TypeAnnotation.Klass) typeAnnotation).cname());
    };
  }

  private static List<jlitec.ast.stmt.Stmt> toAst(
      List<Stmt> stmtList,
      Type expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    final var result = new ArrayList<jlitec.ast.stmt.Stmt>();
    for (final var stmt : stmtList) {
      result.add(toAst(stmt, expectedReturnType, klassDescriptorMap, env));
    }
    return result;
  }

  private static jlitec.ast.stmt.Stmt toAst(
      Stmt stmt,
      Type expectedReturnType,
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
        final var argTypes = new ArrayList<Type>();
        for (final var arg : cs.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env));
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
      Environment env)
      throws SemanticException {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> throw new SemanticException(
          "Trying to call non-callable expression.", "non-callable expression", List.of(target));
      case EXPR_ID -> {
        // local call
        final var ie = (IdExpr) target;
        yield new jlitec.ast.expr.IdExpr(ie.id(), typeAnnotation);
      }
      case EXPR_DOT -> {
        // global call
        final var de = (DotExpr) target;
        final var targetType = typecheck(de.target(), klassDescriptorMap, env);
        final var newTarget =
            switch (de.target().getExprType()) {
              case EXPR_THIS -> new ThisExpr(
                  new TypeAnnotation.Klass(((Type.Klass) env.lookup("this").get()).cname()));
              case EXPR_NEW -> {
                final var ne = (NewExpr) de.target();
                yield new jlitec.ast.expr.NewExpr(ne.cname());
              }
              case EXPR_CALL -> toAst(de.target(), klassDescriptorMap, env);
              case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_DOT, EXPR_ID, EXPR_NULL, EXPR_PAREN -> transformCallTarget(
                  de.target(), typeAnnotation, klassDescriptorMap, env);
            };
        yield new jlitec.ast.expr.DotExpr(newTarget, de.id(), toAst(targetType));
      }
      case EXPR_PAREN -> {
        final var pe = (ParenExpr) target;
        yield transformCallTarget(pe.expr(), typeAnnotation, klassDescriptorMap, env);
      }
    };
  }

  private static MethodReference lookupMethodReference(
      Expr target,
      List<Type> argTypes,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> throw new SemanticException(
          "Trying to call non-callable expression.", "non-callable expression", List.of(target));
        // LocalCall
      case EXPR_ID -> {
        final var ie = (IdExpr) target;
        // `this` must exist in the env
        final var cname = ((Type.Klass) env.lookup("this").get()).cname();
        final var klassDescriptor = klassDescriptorMap.get(cname);
        final var methodDescriptors = klassDescriptor.methods().get(ie.id());
        final var methodDescriptor =
            methodDescriptors.stream()
                .filter(m -> isCompatible(argTypes, m.argTypes()))
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
        final var de = (DotExpr) target;
        // typechecking has already been performed, so casting must succeed
        final var targetType = (Type.Klass) typecheck(de.target(), klassDescriptorMap, env);
        final var klassDescriptor = klassDescriptorMap.get(targetType.cname());
        final var methodDescriptors = klassDescriptor.methods().get(de.id());
        final var methodDescriptor =
            methodDescriptors.stream()
                .filter(m -> isCompatible(argTypes, m.argTypes()))
                .findFirst()
                .get();
        final var returnType = jlitec.ast.Type.fromChecker(methodDescriptor.returnType());
        final var astArgTypes =
            methodDescriptor.argTypes().stream()
                .map(jlitec.ast.Type::fromChecker)
                .collect(Collectors.toUnmodifiableList());
        yield new MethodReference(targetType.cname(), de.id(), returnType, astArgTypes);
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
        final var argTypes = new ArrayList<Type>();
        for (final var arg : ce.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env));
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

  private static TypeAnnotation toAst(Type type) {
    return switch (type.typeEnum()) {
      case INT -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.INT);
      case VOID -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID);
      case STRING -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.STRING);
      case BOOL -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.BOOL);
      case NULL -> new TypeAnnotation.Null();
      case CLASS -> new TypeAnnotation.Klass(((Type.Klass) type).cname());
    };
  }

  private static Type typecheck(
      List<Stmt> stmts,
      Type expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    Type type = Type.VOID;
    for (final var stmt : stmts) {
      type = typecheck(stmt, expectedReturnType, klassDescriptorMap, env);
    }
    return type;
  }

  private static Type typecheck(
      Stmt stmt,
      Type expectedReturnType,
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
                  + rhsType.friendlyName()
                  + "' to variable `"
                  + vas.lhsId()
                  + "' of type `"
                  + expectedType.friendlyName()
                  + "'",
              "incompatible types",
              List.of(vas));
        }
        yield Type.VOID;
      }
      case STMT_FIELD_ASSIGN -> {
        final var fas = (FieldAssignStmt) stmt;
        final var lhsClassType = typecheck(fas.lhsTarget(), klassDescriptorMap, env);
        if (!(lhsClassType instanceof Type.Klass)) {
          throw new SemanticException(
              "Accessing a field of a non-class type `" + lhsClassType.friendlyName() + "'",
              "accessing a field of a non-class type",
              List.of(fas.lhsTarget()));
        }
        final var klass = klassDescriptorMap.get(((Type.Klass) lhsClassType).cname());
        final var maybeFieldType = Optional.ofNullable(klass.fields().get(fas.lhsId()));
        if (maybeFieldType.isEmpty()) {
          throw new SemanticException(
              "Field `"
                  + fas.lhsId()
                  + "' does not exist in class `"
                  + lhsClassType.friendlyName()
                  + "'",
              "non-existent field `" + fas.lhsId() + "'",
              List.of(fas));
        }
        final var expectedType = maybeFieldType.get();
        final var rhsType = typecheck(fas.rhs(), klassDescriptorMap, env);
        if (!isCompatible(expectedType, rhsType)) {
          throw new SemanticException(
              "Incompatible types, trying to assign expression of type `"
                  + rhsType.friendlyName()
                  + "' to field `"
                  + fas.lhsId()
                  + "' of class `"
                  + lhsClassType.friendlyName()
                  + "' of type `"
                  + expectedType.friendlyName()
                  + "'",
              "incompatible types",
              List.of(fas));
        }
        yield Type.VOID;
      }
      case STMT_IF -> {
        final var is = (IfStmt) stmt;
        final var conditionType = typecheck(is.condition(), klassDescriptorMap, env);
        if (conditionType.typeEnum() != Type.TypeEnum.BOOL) {
          throw new SemanticException(
              "If condition expression must be of type `Bool', but encountered `"
                  + conditionType.friendlyName()
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
                  + thenType.friendlyName()
                  + "', else block type is `"
                  + elseType.friendlyName()
                  + "'",
              "incompatible then and else type",
              List.of(is));
        }
        yield elseType;
      }
      case STMT_WHILE -> {
        final var ws = (WhileStmt) stmt;
        final var conditionType = typecheck(ws.condition(), klassDescriptorMap, env);
        if (conditionType.typeEnum() != Type.TypeEnum.BOOL) {
          throw new SemanticException(
              "While condition expression must be of type `Bool', but encountered `"
                  + conditionType.friendlyName()
                  + "'",
              "condition type is not `Bool'",
              List.of(ws.condition()));
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
        if (!Set.of(Type.TypeEnum.INT, Type.TypeEnum.BOOL, Type.TypeEnum.STRING)
            .contains(varType.typeEnum())) {
          throw new SemanticException(
              "Type of variable passed to `readln' must be `Int', `Bool', or `String', but encountered `"
                  + varType.friendlyName()
                  + "'",
              "incompatible type",
              List.of(rs));
        }
        yield Type.VOID;
      }
      case STMT_PRINTLN -> {
        final var ps = (PrintlnStmt) stmt;
        final var psType = typecheck(ps.expr(), klassDescriptorMap, env);
        if (!Set.of(Type.TypeEnum.INT, Type.TypeEnum.BOOL, Type.TypeEnum.STRING)
            .contains(psType.typeEnum())) {
          throw new SemanticException(
              "Type of expression passed to `println' must be `Int', `Bool', or `String', but encountered `"
                  + psType.friendlyName()
                  + "'",
              "incompatible type",
              List.of(ps));
        }
        yield Type.VOID;
      }
      case STMT_CALL -> {
        final var cs = (CallStmt) stmt;
        final var argTypes = new ArrayList<Type>();
        for (final var arg : cs.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env));
        }
        yield lookupMethodReturnType(cs.target(), argTypes, klassDescriptorMap, env);
      }
      case STMT_RETURN -> {
        final var rs = (ReturnStmt) stmt;
        final var maybeExpr = rs.maybeExpr();
        if (maybeExpr.isEmpty()) {
          if (expectedReturnType.typeEnum() != Type.TypeEnum.VOID) {
            throw new SemanticException(
                "Returning `Void' for a method that expects return type `"
                    + expectedReturnType.friendlyName()
                    + "'",
                "invalid return type",
                List.of(rs));
          }
          yield Type.VOID;
        }
        final var expr = maybeExpr.get();
        final var exprType = typecheck(expr, klassDescriptorMap, env);
        if (!isCompatible(expectedReturnType, exprType)) {
          throw new SemanticException(
              "Returning `"
                  + exprType.friendlyName()
                  + "' for a method that expects return type `"
                  + expectedReturnType.friendlyName()
                  + "'",
              "invalid return type",
              List.of(rs));
        }
        yield exprType;
      }
    };
  }

  private static Type typecheck(
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
      case EXPR_INT_LITERAL -> Type.INT;
      case EXPR_BOOL_LITERAL -> Type.BOOL;
      case EXPR_STRING_LITERAL -> Type.STRING;
      case EXPR_DOT -> {
        final var de = (DotExpr) expr;
        final var targetType = typecheck(de.target(), klassDescriptorMap, env);
        if (!(targetType instanceof Type.Klass)) {
          throw new SemanticException(
              "Accessing a field of a non-class type `" + targetType.friendlyName() + "'",
              "accessing a field of a non-class type",
              List.of(de.target()));
        }
        final var klassDescriptor = klassDescriptorMap.get(((Type.Klass) targetType).cname());
        final var maybeFieldType =
            Optional.ofNullable(klassDescriptor.fields().getOrDefault(de.id(), null));
        if (maybeFieldType.isEmpty()) {
          throw new SemanticException(
              "Field `" + de.id() + "' does not exist in class `" + targetType.friendlyName() + "'",
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
        yield new Type.Klass(((NewExpr) expr).cname());
      }
      case EXPR_CALL -> {
        final var ce = (CallExpr) expr;
        final var argTypes = new ArrayList<Type>();
        for (final var arg : ce.args()) {
          argTypes.add(typecheck(arg, klassDescriptorMap, env));
        }
        yield lookupMethodReturnType(ce.target(), argTypes, klassDescriptorMap, env);
      }
      case EXPR_BINARY -> {
        final var be = (BinaryExpr) expr;
        final var lhsType = typecheck(be.lhs(), klassDescriptorMap, env);
        final var rhsType = typecheck(be.rhs(), klassDescriptorMap, env);
        yield switch (be.op()) {
          case PLUS -> {
            final var stringTypes = Set.of(Type.TypeEnum.STRING, Type.TypeEnum.NULL);
            if (lhsType.typeEnum() == Type.TypeEnum.INT) {
              if (rhsType.typeEnum() != Type.TypeEnum.INT) {
                throw new SemanticException(
                    "Arithmetic binary operator `+' second operand must be `Int' but encountered `"
                        + rhsType.friendlyName()
                        + "'",
                    "wrong operand type for arithmetic binary operator",
                    List.of(be.rhs()));
              }
              yield Type.INT;
            } else if (stringTypes.contains(lhsType.typeEnum())) {
              if (stringTypes.contains(rhsType.typeEnum())) {
                yield Type.STRING;
              } else {
                throw new SemanticException(
                    "String concatenation operator `+' second operand must be `String' or `null' but encountered `"
                        + rhsType.friendlyName()
                        + "'",
                    "wrong operand type for string concatenation",
                    List.of(be.rhs()));
              }
            } else {
              throw new SemanticException(
                  "Invalid first operand type for binary operator `+', expected either `Int', `String', or `null' but encountered `"
                      + lhsType.friendlyName()
                      + "'",
                  "wrong operand type for `+'",
                  List.of(be));
            }
          }
          case MINUS, MULT, DIV -> {
            if (lhsType.typeEnum() != Type.TypeEnum.INT) {
              throw new SemanticException(
                  "Arithmetic binary operator `"
                      + be.op().toString()
                      + "' first operand must be `Int' but encountered `"
                      + lhsType.friendlyName()
                      + "'",
                  "wrong operand type for arithmetic binary operator",
                  List.of(be.lhs()));
            }
            if (rhsType.typeEnum() != Type.TypeEnum.INT) {
              throw new SemanticException(
                  "Arithmetic binary operator `"
                      + be.op().toString()
                      + "' second operand must be `Int' but encountered `"
                      + rhsType.friendlyName()
                      + "'",
                  "wrong operand type for arithmetic binary operator",
                  List.of(be.rhs()));
            }
            yield Type.INT;
          }
          case LT, GT, LEQ, GEQ, EQ, NEQ -> {
            if (lhsType.typeEnum() != Type.TypeEnum.INT) {
              throw new SemanticException(
                  "Comparison binary operator `"
                      + be.op().toString()
                      + "' first operand must be `Int' but encountered `"
                      + lhsType.friendlyName()
                      + "'",
                  "wrong operand type for comparison binary operator",
                  List.of(be.lhs()));
            }
            if (rhsType.typeEnum() != Type.TypeEnum.INT) {
              throw new SemanticException(
                  "Comparison binary operator `"
                      + be.op().toString()
                      + "' second operand must be `Int' but encountered `"
                      + rhsType.friendlyName()
                      + "'",
                  "wrong operand type for comparison binary operator",
                  List.of(be.rhs()));
            }
            yield Type.BOOL;
          }
          case AND, OR -> {
            if (lhsType.typeEnum() != Type.TypeEnum.BOOL) {
              throw new SemanticException(
                  "Boolean binary operator `"
                      + be.op().toString()
                      + "' first operand must be `Bool' but encountered `"
                      + lhsType.friendlyName()
                      + "'",
                  "wrong operand type for boolean binary operator",
                  List.of(be.lhs()));
            }
            if (rhsType.typeEnum() != Type.TypeEnum.BOOL) {
              throw new SemanticException(
                  "Boolean binary operator `"
                      + be.op().toString()
                      + "' second operand must be `Bool' but encountered `"
                      + rhsType.friendlyName()
                      + "'",
                  "wrong operand type for boolean binary operator",
                  List.of(be.rhs()));
            }
            yield Type.BOOL;
          }
        };
      }
      case EXPR_UNARY -> {
        final var ue = (UnaryExpr) expr;
        final var exprType = typecheck(ue.expr(), klassDescriptorMap, env);
        yield switch (ue.op()) {
          case NEGATIVE -> {
            if (exprType.typeEnum() != Type.TypeEnum.INT) {
              throw new SemanticException(
                  "Negation can only be applied to `Int` type, but encountered `"
                      + exprType.friendlyName()
                      + "'",
                  "invalid type for negation",
                  List.of(ue));
            }
            yield Type.INT;
          }
          case NOT -> {
            if (exprType.typeEnum() != Type.TypeEnum.BOOL) {
              throw new SemanticException(
                  "Complement can only be applied to `Bool` type, but encountered `"
                      + exprType.friendlyName()
                      + "'",
                  "invalid type for negation",
                  List.of(ue));
            }
            yield Type.BOOL;
          }
        };
      }
      case EXPR_NULL -> Type.NULL;
    };
  }

  private static Type lookupMethodReturnType(
      Expr target,
      List<Type> argTypes,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    return switch (target.getExprType()) {
      case EXPR_INT_LITERAL, EXPR_STRING_LITERAL, EXPR_BOOL_LITERAL, EXPR_BINARY, EXPR_UNARY, EXPR_THIS, EXPR_CALL, EXPR_NEW, EXPR_NULL -> throw new SemanticException(
          "Trying to call non-callable expression.", "non-callable expression", List.of(target));
        // LocalCall
      case EXPR_ID -> {
        final var ie = (IdExpr) target;
        // `this` must exist in the env
        final var cname = ((Type.Klass) env.lookup("this").get()).cname();
        final var klassDescriptor = klassDescriptorMap.get(cname);
        final var maybeMethods = Optional.ofNullable(klassDescriptor.methods().get(ie.id()));
        final var friendlyArgTypes =
            argTypes.stream().map(Type::friendlyName).collect(Collectors.toUnmodifiableList());
        final var signature = ie.id() + "(" + String.join(", ", friendlyArgTypes) + ")";
        if (maybeMethods.isEmpty()) {
          throw new SemanticException(
              "Local method `" + signature + "' does not exist.",
              "non-existent local method `" + signature + "'",
              List.of(ie));
        }
        final var returnTypes =
            maybeMethods.get().stream()
                .filter(m -> isCompatible(argTypes, m.argTypes()))
                .collect(Collectors.toUnmodifiableList());
        if (returnTypes.isEmpty()) {
          throw new SemanticException(
              "Local method `" + signature + "' does not exist.",
              "non-existent method `" + signature + "'",
              List.of(ie));
        }
        if (returnTypes.size() > 1) {
          final var possibleMethods =
              returnTypes.stream()
                  .map(
                      m ->
                          ie.id()
                              + "("
                              + m.argTypes().stream()
                                  .map(Type::friendlyName)
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
        if (targetType.equals(Type.NULL)) {
          throw new SemanticException(
              "Trying to access a method of `null'", "calling a method on `null'", List.of(target));
        }
        if (!(targetType instanceof Type.Klass)) {
          throw new SemanticException(
              "Accessing a method of a non-class type`" + targetType.friendlyName() + "'",
              "accessing a method of a non-class type",
              List.of(de.target()));
        }
        final var klassDescriptor = klassDescriptorMap.get(((Type.Klass) targetType).cname());
        final var maybeMethods = Optional.ofNullable(klassDescriptor.methods().get(de.id()));
        final var friendlyArgTypes =
            argTypes.stream().map(Type::friendlyName).collect(Collectors.toUnmodifiableList());
        final var signature = de.id() + "(" + String.join(", ", friendlyArgTypes) + ")";
        if (maybeMethods.isEmpty()) {
          throw new SemanticException(
              "Method `"
                  + signature
                  + "' is not found on class `"
                  + targetType.friendlyName()
                  + "'",
              "non-existent method `" + signature + "'",
              List.of(de));
        }
        final var returnTypes =
            maybeMethods.get().stream()
                .filter(m -> isCompatible(argTypes, m.argTypes()))
                .collect(Collectors.toUnmodifiableList());
        if (returnTypes.isEmpty()) {
          throw new SemanticException(
              "Method `"
                  + signature
                  + "' is not found on class `"
                  + targetType.friendlyName()
                  + "'",
              "non-existent method `" + signature + "'",
              List.of(de));
        }
        if (returnTypes.size() > 1) {
          final var possibleMethods =
              returnTypes.stream()
                  .map(
                      m ->
                          de.id()
                              + "("
                              + m.argTypes().stream()
                                  .map(Type::friendlyName)
                                  .collect(Collectors.joining(", "))
                              + ")")
                  .collect(Collectors.joining(", "));
          throw new SemanticException(
              "Call with signature `"
                  + signature
                  + "' on class `"
                  + targetType.friendlyName()
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

  private record TypeTuple(Type type1, Type type2) {}

  private static boolean isCompatible(List<Type> actualTypes, List<Type> expectedTypes) {
    if (actualTypes.size() != expectedTypes.size()) {
      return false;
    }

    return Streams.zip(actualTypes.stream(), expectedTypes.stream(), TypeTuple::new)
        .allMatch(tuple -> isCompatible(tuple.type1, tuple.type2));
  }

  private static boolean isCompatible(Type type1, Type type2) {
    return switch (type1.typeEnum()) {
        // primitive types must be equal
      case VOID, INT, BOOL -> type1.equals(type2);
        // null can only match non-primitive types
      case NULL -> !Set.of(Type.TypeEnum.VOID, Type.TypeEnum.INT, Type.TypeEnum.BOOL)
          .contains(type2.typeEnum());
        // objects can only match other object of the same type or null
      case STRING, CLASS -> type1.equals(type2) || type2.typeEnum() == Type.TypeEnum.NULL;
    };
  }

  public static void distinctNameCheck(Program program) throws SemanticException {
    distinctClassNameCheck(program);
    for (final var klass : program.klassList()) {
      distinctFieldNameCheck(klass);
      distinctMethodNameCheck(klass);
      distinctParamNameCheck(klass);
      for (final var method : klass.methods()) {
        distinctArgLocalNameCheck(method);
      }
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

  private static void distinctArgLocalNameCheck(Method method) throws SemanticException {
    final var grouped =
        Stream.concat(method.args().stream(), method.vars().stream())
            .collect(Collectors.groupingBy(v -> v.name().id()));
    for (final var entry : grouped.entrySet()) {
      if (entry.getValue().size() > 1) {
        throw new SemanticException(
            "Variables with the same name in method arguments and/or local declarations: `"
                + entry.getKey()
                + "'",
            "duplicate arg and/or local variable: `" + entry.getKey() + "'",
            entry.getValue());
      }
    }
  }
}
