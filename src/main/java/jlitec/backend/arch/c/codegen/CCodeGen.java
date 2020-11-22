package jlitec.backend.arch.c.codegen;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.backend.arch.c.Method;
import jlitec.backend.arch.c.Program;
import jlitec.backend.arch.c.Struct;
import jlitec.backend.arch.c.Type;
import jlitec.backend.arch.c.Var;
import jlitec.backend.arch.c.expr.BinaryExpr;
import jlitec.backend.arch.c.expr.BinaryOp;
import jlitec.backend.arch.c.expr.BoolLiteralExpr;
import jlitec.backend.arch.c.expr.CallExpr;
import jlitec.backend.arch.c.expr.Expr;
import jlitec.backend.arch.c.expr.FieldExpr;
import jlitec.backend.arch.c.expr.IdExpr;
import jlitec.backend.arch.c.expr.IntLiteralExpr;
import jlitec.backend.arch.c.expr.NullExpr;
import jlitec.backend.arch.c.expr.StringLiteralExpr;
import jlitec.backend.arch.c.expr.UnaryExpr;
import jlitec.backend.arch.c.expr.UnaryOp;
import jlitec.backend.arch.c.stmt.CallStmt;
import jlitec.backend.arch.c.stmt.FieldAssignStmt;
import jlitec.backend.arch.c.stmt.GotoStmt;
import jlitec.backend.arch.c.stmt.IfStmt;
import jlitec.backend.arch.c.stmt.LabelStmt;
import jlitec.backend.arch.c.stmt.ReturnStmt;
import jlitec.backend.arch.c.stmt.Stmt;
import jlitec.backend.arch.c.stmt.VarAssignStmt;
import jlitec.ir3.expr.NewExpr;
import jlitec.ir3.expr.rval.IdRvalExpr;

public class CCodeGen {
  // prevent instantiation
  private CCodeGen() {}

  public static Program gen(jlitec.ir3.Program program) {
    final var structs =
        program.dataList().stream().map(CCodeGen::gen).collect(Collectors.toUnmodifiableList());
    final var methods =
        program.methodList().stream().map(CCodeGen::gen).collect(Collectors.toUnmodifiableList());
    return new Program(structs, methods);
  }

  private static Struct gen(jlitec.ir3.Data data) {
    return new Struct(
        data.cname(),
        data.fields().stream().map(CCodeGen::gen).collect(Collectors.toUnmodifiableList()));
  }

  private static Var gen(jlitec.ir3.Var var) {
    return new Var(gen(var.type()), var.id());
  }

  private static Type gen(jlitec.ir3.Type type) {
    return switch (type.type()) {
      case BOOL -> Type.BOOL;
      case INT -> Type.INT;
      case VOID -> Type.VOID;
      case STRING -> Type.CHAR_ARRAY;
      case CLASS -> new Type.Struct(((jlitec.ir3.Type.KlassType) type).cname());
    };
  }

  private static Method gen(jlitec.ir3.Method method) {
    final var tempVarGen = new TempVarGen();
    final var args =
        Stream.concat(
                Stream.of(new Var(new Type.Struct(method.cname()), "this")),
                method.args().stream().map(CCodeGen::gen))
            .collect(Collectors.toUnmodifiableList());
    final var typeMap =
        Stream.concat(method.vars().stream(), method.args().stream())
            .map(CCodeGen::gen)
            .collect(Collectors.toUnmodifiableMap(Var::id, Var::type));
    final var stmtList =
        method.stmtList().stream()
            .flatMap(stmt -> CCodeGen.gen(stmt, typeMap, tempVarGen).stream())
            .collect(Collectors.toUnmodifiableList());
    final var vars =
        Stream.concat(method.vars().stream().map(CCodeGen::gen), tempVarGen.getAllVars().stream())
            .collect(Collectors.toUnmodifiableList());
    return new Method(method.id().replace("%", ""), gen(method.returnType()), args, vars, stmtList);
  }

  private static List<Stmt> gen(
      jlitec.ir3.stmt.Stmt stmt, Map<String, Type> typeMap, TempVarGen gen) {
    return switch (stmt.getStmtType()) {
      case LABEL -> List.of(new LabelStmt(((jlitec.ir3.stmt.LabelStmt) stmt).label()));
      case GOTO -> List.of(new GotoStmt(((jlitec.ir3.stmt.GotoStmt) stmt).dest().label()));
      case READLN -> {
        final var rs = (jlitec.ir3.stmt.ReadlnStmt) stmt;
        yield switch (typeMap.get(rs.dest().id()).type()) {
          case INT -> List.of(
              new VarAssignStmt(rs.dest().id(), new CallExpr("readln_int", List.of())));
          case BOOL -> List.of(
              new VarAssignStmt(rs.dest().id(), new CallExpr("readln_bool", List.of())));
          case CHAR_ARRAY -> List.of(
              new VarAssignStmt(
                  rs.dest().id(), new CallExpr("getline_without_newline", List.of())));
          case STRUCT, VOID -> throw new RuntimeException("should have failed typecheck");
        };
      }
      case PRINTLN -> {
        final var ps = (jlitec.ir3.stmt.PrintlnStmt) stmt;
        yield switch (ps.rval().getRvalExprType()) {
          case ID -> {
            final var ir = (jlitec.ir3.expr.rval.IdRvalExpr) ps.rval();
            final var format =
                switch (typeMap.get(ir.id()).type()) {
                  case BOOL, INT -> new StringLiteralExpr("%d\n");
                  case CHAR_ARRAY -> new StringLiteralExpr("%s\n");
                  case STRUCT, VOID -> throw new RuntimeException("should have failed typecheck");
                };
            yield List.of(new CallStmt("printf", List.of(format, new IdExpr(ir.id()))));
          }
          case BOOL -> {
            final var be = (jlitec.ir3.expr.rval.BoolRvalExpr) ps.rval();
            yield List.of(
                new CallStmt(
                    "printf",
                    List.of(new StringLiteralExpr("%d\n"), new BoolLiteralExpr(be.value()))));
          }
          case INT -> {
            final var ie = (jlitec.ir3.expr.rval.IntRvalExpr) ps.rval();
            yield List.of(
                new CallStmt(
                    "printf",
                    List.of(new StringLiteralExpr("%d\n"), new IntLiteralExpr(ie.value()))));
          }
          case STRING -> {
            final var se = (jlitec.ir3.expr.rval.StringRvalExpr) ps.rval();
            yield List.of(
                new CallStmt("printf", List.of(new StringLiteralExpr(se.value() + "\n"))));
          }
          case NULL -> List.of(new CallStmt("printf", List.of(new StringLiteralExpr("null\n"))));
        };
      }
      case CMP -> {
        final var cs = (jlitec.ir3.stmt.CmpStmt) stmt;
        final var conditionChunk = gen(cs.condition(), typeMap, gen);
        yield ImmutableList.<Stmt>builder()
            .addAll(conditionChunk.stmtList())
            .add(new IfStmt(conditionChunk.expr(), cs.dest().label()))
            .build();
      }
      case VAR_ASSIGN -> {
        final var vas = (jlitec.ir3.stmt.VarAssignStmt) stmt;
        final var rhsChunk = gen(vas.rhs(), typeMap, gen);
        yield ImmutableList.<Stmt>builder()
            .addAll(rhsChunk.stmtList())
            .add(new VarAssignStmt(vas.lhs().id(), rhsChunk.expr()))
            .build();
      }
      case FIELD_ASSIGN -> {
        final var fas = (jlitec.ir3.stmt.FieldAssignStmt) stmt;
        final var rhsChunk = gen(fas.rhs(), typeMap, gen);
        yield ImmutableList.<Stmt>builder()
            .addAll(rhsChunk.stmtList())
            .add(new FieldAssignStmt(fas.lhsId().id(), fas.lhsField(), rhsChunk.expr()))
            .build();
      }
      case CALL -> {
        final var cs = (jlitec.ir3.stmt.CallStmt) stmt;
        final var argChunks =
            cs.args().stream()
                .map(e -> CCodeGen.gen(e, typeMap, gen))
                .collect(Collectors.toUnmodifiableList());
        final var argsStmtList =
            argChunks.stream()
                .flatMap(c -> c.stmtList().stream())
                .collect(Collectors.toUnmodifiableList());
        final var args =
            argChunks.stream().map(ExprChunk::expr).collect(Collectors.toUnmodifiableList());
        yield ImmutableList.<Stmt>builder()
            .addAll(argsStmtList)
            .add(new CallStmt(cs.target().id().replace("%", ""), args))
            .build();
      }
      case RETURN -> {
        final var rs = (jlitec.ir3.stmt.ReturnStmt) stmt;
        final var maybeId = rs.maybeValue().map(IdRvalExpr::id);
        yield List.of(new ReturnStmt(maybeId));
      }
    };
  }

  private record StringChunk(Expr expr, Expr size) {}

  private static ExprChunk gen(
      jlitec.ir3.expr.Expr expr, Map<String, Type> typeMap, TempVarGen gen) {
    return switch (expr.getExprType()) {
      case BINARY -> {
        final var be = (jlitec.ir3.expr.BinaryExpr) expr;
        if (be.op() == jlitec.ir3.expr.BinaryOp.PLUS) {
          final var isLhsString =
              switch (be.lhs().getRvalExprType()) {
                case BOOL, INT -> false;
                case STRING, NULL -> true;
                case ID -> typeMap.get(((jlitec.ir3.expr.rval.IdRvalExpr) be.lhs()).id())
                    == Type.CHAR_ARRAY;
              };
          if (isLhsString) {
            final var lhs =
                switch (be.lhs().getRvalExprType()) {
                  case BOOL, INT -> throw new RuntimeException("should not be reached");
                  case STRING -> {
                    final var sre = (jlitec.ir3.expr.rval.StringRvalExpr) be.lhs();
                    yield new StringChunk(
                        new StringLiteralExpr(sre.value()),
                        new IntLiteralExpr(sre.value().length() + 1));
                  }
                  case NULL -> new StringChunk(new StringLiteralExpr(""), new IntLiteralExpr(1));
                  case ID -> {
                    final var ire = (jlitec.ir3.expr.rval.IdRvalExpr) be.lhs();
                    yield new StringChunk(
                        new IdExpr(ire.id()),
                        new CallExpr("strlen", List.of(new IdExpr(ire.id()))));
                  }
                };
            final var rhs =
                switch (be.rhs().getRvalExprType()) {
                  case BOOL, INT -> throw new RuntimeException("should not be reached");
                  case STRING -> {
                    final var sre = (jlitec.ir3.expr.rval.StringRvalExpr) be.rhs();
                    yield new StringChunk(
                        new StringLiteralExpr(sre.value()),
                        new IntLiteralExpr(sre.value().length() + 1));
                  }
                  case NULL -> new StringChunk(new StringLiteralExpr(""), new IntLiteralExpr(1));
                  case ID -> {
                    final var ire = (jlitec.ir3.expr.rval.IdRvalExpr) be.rhs();
                    yield new StringChunk(
                        new IdExpr(ire.id()),
                        new CallExpr("strlen", List.of(new IdExpr(ire.id()))));
                  }
                };
            final var tempVar = gen.gen(Type.CHAR_ARRAY);
            yield new ExprChunk(
                new IdExpr(tempVar.id()),
                ImmutableList.<Stmt>builder()
                    .add(
                        new VarAssignStmt(
                            tempVar.id(),
                            new CallExpr(
                                "malloc",
                                List.of(
                                    new BinaryExpr(
                                        BinaryOp.MINUS,
                                        new BinaryExpr(BinaryOp.PLUS, lhs.size(), rhs.size()),
                                        new IntLiteralExpr(1))))))
                    .add(new CallStmt("strcpy", List.of(new IdExpr(tempVar.id()), lhs.expr())))
                    .add(new CallStmt("strcat", List.of(new IdExpr(tempVar.id()), rhs.expr())))
                    .build());
          }
        }
        final var lhsChunk = gen(be.lhs(), typeMap, gen);
        final var rhsChunk = gen(be.rhs(), typeMap, gen);
        yield new ExprChunk(
            new BinaryExpr(BinaryOp.fromIr3(be.op()), lhsChunk.expr(), rhsChunk.expr()),
            ImmutableList.<Stmt>builder()
                .addAll(lhsChunk.stmtList())
                .addAll(rhsChunk.stmtList())
                .build());
      }
      case UNARY -> {
        final var ue = (jlitec.ir3.expr.UnaryExpr) expr;
        final var exprChunk = gen(ue.rval(), typeMap, gen);
        yield new ExprChunk(
            new UnaryExpr(UnaryOp.fromIr3(ue.op()), exprChunk.expr()), exprChunk.stmtList());
      }
      case FIELD -> {
        final var fe = (jlitec.ir3.expr.FieldExpr) expr;
        yield new ExprChunk(new FieldExpr(fe.target().id(), fe.field()), List.of());
      }
      case RVAL -> {
        final var re = (jlitec.ir3.expr.rval.RvalExpr) expr;
        yield switch (re.getRvalExprType()) {
          case NULL -> new ExprChunk(new NullExpr(), List.of());
          case BOOL -> new ExprChunk(
              new BoolLiteralExpr(((jlitec.ir3.expr.rval.BoolRvalExpr) re).value()), List.of());
          case INT -> new ExprChunk(
              new IntLiteralExpr(((jlitec.ir3.expr.rval.IntRvalExpr) re).value()), List.of());
          case ID -> new ExprChunk(
              new IdExpr(((jlitec.ir3.expr.rval.IdRvalExpr) re).id()), List.of());
          case STRING -> {
            final var se = (jlitec.ir3.expr.rval.StringRvalExpr) re;
            final var tempVar = gen.gen(Type.CHAR_ARRAY);
            yield new ExprChunk(
                new IdExpr(tempVar.id()),
                ImmutableList.<Stmt>builder()
                    .add(
                        new VarAssignStmt(
                            tempVar.id(),
                            new CallExpr(
                                "malloc", List.of(new IntLiteralExpr(se.value().length() + 1)))))
                    .add(
                        new CallStmt(
                            "strcpy",
                            List.of(new IdExpr(tempVar.id()), new StringLiteralExpr(se.value()))))
                    .build());
          }
        };
      }
      case CALL -> {
        final var ce = (jlitec.ir3.expr.CallExpr) expr;
        final var argChunks =
            ce.args().stream()
                .map(e -> CCodeGen.gen(e, typeMap, gen))
                .collect(Collectors.toUnmodifiableList());
        final var argsStmt =
            argChunks.stream()
                .flatMap(c -> c.stmtList().stream())
                .collect(Collectors.toUnmodifiableList());
        final var args =
            argChunks.stream().map(ExprChunk::expr).collect(Collectors.toUnmodifiableList());
        yield new ExprChunk(new CallExpr(ce.target().id().replace("%", ""), args), argsStmt);
      }
      case NEW -> {
        // huehuehue let's just LEAK STUFFS!
        final var ne = (NewExpr) expr;
        yield new ExprChunk(
            new CallExpr(
                "malloc",
                List.of(new CallExpr("sizeof", List.of(new IdExpr("struct " + ne.cname()))))),
            List.of());
      }
    };
  }
}
