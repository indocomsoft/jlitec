package jlitec.ir3;

import java.util.Optional;
import jlitec.Printable;
import jlitec.ast.TypeAnnotation;

public interface Type extends Printable {
  Ir3Type type();

  record PrimitiveType(Ir3Type type) implements Type {
    @Override
    public String print(int indent) {
      return switch (type) {
        case INT -> "Int";
        case BOOL -> "Bool";
        case STRING -> "String";
        case VOID -> "Void";
        case CLASS -> throw new RuntimeException("Trying to print a class type.");
      };
    }
  }

  record KlassType(String cname) implements Type {
    @Override
    public Ir3Type type() {
      return Ir3Type.CLASS;
    }

    @Override
    public String print(int indent) {
      return cname;
    }
  }

  static Type fromAst(jlitec.ast.Type type) {
    return switch (type.jliteType()) {
      case INT -> new PrimitiveType(Ir3Type.INT);
      case BOOL -> new PrimitiveType(Ir3Type.BOOL);
      case STRING -> new PrimitiveType(Ir3Type.STRING);
      case VOID -> new PrimitiveType(Ir3Type.VOID);
      case CLASS -> new KlassType(((jlitec.ast.KlassType) type).cname());
    };
  }

  static Optional<Type> fromTypeAnnotation(jlitec.ast.TypeAnnotation typeAnnotation) {
    return Optional.ofNullable(
        switch (typeAnnotation.annotation()) {
          case INT -> new PrimitiveType(Ir3Type.INT);
          case STRING -> new PrimitiveType(Ir3Type.STRING);
          case VOID -> new PrimitiveType(Ir3Type.VOID);
          case BOOL -> new PrimitiveType(Ir3Type.BOOL);
          case CLASS -> new KlassType(((TypeAnnotation.Klass) typeAnnotation).cname());
          case NULL -> null;
        });
  }
}
