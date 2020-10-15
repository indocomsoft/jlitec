package jlitec.checker;

public interface Type {
  TypeEnum typeEnum();

  String friendlyName();

  Type NULL = new Basic(TypeEnum.NULL);

  record Basic(TypeEnum typeEnum) implements Type {
    @Override
    public String friendlyName() {
      return switch (typeEnum) {
        case CLASS -> throw new RuntimeException("Should not be reached");
        case VOID -> "Void";
        case INT -> "Int";
        case STRING -> "String";
        case BOOL -> "Bool";
        case NULL -> "null";
      };
    }
  }

  record Klass(String cname) implements Type {
    @Override
    public TypeEnum typeEnum() {
      return TypeEnum.CLASS;
    }

    @Override
    public String friendlyName() {
      return cname;
    }
  }

  enum TypeEnum {
    CLASS,
    VOID,
    INT,
    STRING,
    BOOL,
    NULL;
  }

  static Type fromParseTree(jlitec.parsetree.Type type) {
    return switch (type.type()) {
      case INT -> new Basic(TypeEnum.INT);
      case STRING -> new Basic(TypeEnum.STRING);
      case BOOL -> new Basic(TypeEnum.BOOL);
      case VOID -> new Basic(TypeEnum.VOID);
      case CLASS -> new Klass(((jlitec.parsetree.KlassType) type).cname());
    };
  }
}
