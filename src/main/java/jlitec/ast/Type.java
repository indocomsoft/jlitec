package jlitec.ast;

public interface Type {
  JliteType jliteType();

  static Type fromParseTree(jlitec.parsetree.Type type) {
    return switch (type.type()) {
      case INT -> new PrimitiveType(JliteType.INT);
      case BOOL -> new PrimitiveType(JliteType.BOOL);
      case STRING -> new PrimitiveType(JliteType.STRING);
      case VOID -> new PrimitiveType(JliteType.VOID);
      case CLASS -> new KlassType(((jlitec.parsetree.KlassType) type).cname());
    };
  }

  static Type fromChecker(jlitec.checker.Type.Basic basicType) {
    final var type = basicType.type();
    if (type.equals(jlitec.parsetree.JliteType.VOID.print(0))) {
      return new PrimitiveType(JliteType.VOID);
    } else if (type.equals(jlitec.parsetree.JliteType.INT.print(0))) {
      return new PrimitiveType(JliteType.INT);
    } else if (type.equals(jlitec.parsetree.JliteType.STRING.print(0))) {
      return new PrimitiveType(JliteType.STRING);
    } else if (type.equals(jlitec.parsetree.JliteType.BOOL.print(0))) {
      return new PrimitiveType(JliteType.BOOL);
    } else if (basicType.equals(jlitec.checker.Type.Basic.NULL)) {
      throw new RuntimeException("Trying to convert null to an ast type");
    }
    return new KlassType(type);
  }
}
