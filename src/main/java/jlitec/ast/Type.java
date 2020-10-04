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
}
