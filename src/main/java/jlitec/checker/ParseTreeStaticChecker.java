package jlitec.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jlitec.parsetree.JliteType;
import jlitec.parsetree.Klass;
import jlitec.parsetree.KlassType;
import jlitec.parsetree.Method;
import jlitec.parsetree.Program;
import jlitec.parsetree.Var;
import jlitec.parsetree.expr.BinaryExpr;
import jlitec.parsetree.expr.CallExpr;
import jlitec.parsetree.expr.DotExpr;
import jlitec.parsetree.expr.Expr;
import jlitec.parsetree.expr.IdExpr;
import jlitec.parsetree.expr.NewExpr;
import jlitec.parsetree.expr.ParenExpr;
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

  public static void cnameTypeExistenceCheck(
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

  public static void typecheck(Program program, Map<String, KlassDescriptor> klassDescriptorMap)
      throws SemanticException {
    Environment env = new Environment();
    for (final var klass : program.klassList()) {
      typecheck(klass, klassDescriptorMap, env);
    }
  }

  public static void typecheck(
      Klass klass, Map<String, KlassDescriptor> klassDescriptorMap, Environment env)
      throws SemanticException {
    final var klassName = klass.name().id();
    env = env.augment(new Environment(klassDescriptorMap.get(klassName)));
    env = env.augment("this", new Type.Basic(klassName));
    for (final var method : klass.methods()) {
      typecheck(method, klassDescriptorMap, env);
    }
  }

  public static void typecheck(
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

  public static Type.Basic typecheck(
      List<Stmt> stmts,
      Type.Basic expectedReturnType,
      Map<String, KlassDescriptor> klassDescriptorMap,
      Environment env)
      throws SemanticException {
    Type.Basic type = new Type.Basic(JliteType.VOID);
    for (final var stmt : stmts) {
      type =
          switch (stmt.getStmtType()) {
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
                    "Field `"
                        + fas.lhsId()
                        + "' does not exist in class `"
                        + lhsClassType.type()
                        + "'",
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
              if (!Set.of(
                      JliteType.INT.print(0), JliteType.BOOL.print(0), JliteType.STRING.print(0))
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
              if (!Set.of(
                      JliteType.INT.print(0), JliteType.BOOL.print(0), JliteType.STRING.print(0))
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
    return type;
  }

  public static Type.Basic typecheck(
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
                        + lhsType.type()
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

  public static Type.Basic lookupMethodReturnType(
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
        final var maybeType = env.lookup(new Environment.MethodDescriptor(ie.id(), argTypes));
        if (maybeType.isEmpty()) {
          throw new SemanticException(
              "Local method `" + ie.id() + "' does not exist.",
              "non-existent local method `" + ie.id() + "'",
              List.of(ie));
        }
        yield maybeType.get();
      }
        // GlobalCall
      case EXPR_DOT -> {
        final var de = (DotExpr) target;
        final var targetType = typecheck(de.target(), klassDescriptorMap, env);
        final var klassDescriptor = klassDescriptorMap.get(targetType.type());
        final var maybeMethods = Optional.ofNullable(klassDescriptor.methods().get(de.id()));
        if (maybeMethods.isEmpty()) {
          final var signature = de.id() + "(" + String.join(", ", argTypes) + ")";
          throw new SemanticException(
              "Method `" + signature + "' is not found on class `" + targetType.type() + "'",
              "non-existent method `" + signature + "'",
              List.of(de));
        }
        final var maybeReturnType =
            maybeMethods.get().stream()
                .filter(
                    m ->
                        isCompatible(
                            argTypes,
                            m.argTypes().stream()
                                .map(Type.Basic::type)
                                .collect(Collectors.toUnmodifiableList())))
                .findFirst();
        if (maybeReturnType.isEmpty()) {
          final var signature = de.id() + "(" + String.join(", ", argTypes) + ")";
          throw new SemanticException(
              "Method `" + signature + "' is not found on class `" + targetType.type() + "'",
              "non-existent method `" + signature + "'",
              List.of(de));
        }
        yield maybeReturnType.get().returnType();
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

  private static void distinctNameCheck(Program program) throws SemanticException {
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
                  + paramList.get(0).name().id(),
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
