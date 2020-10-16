package jlitec.backend.c;

import jlitec.Printable;

public interface Type extends Printable {
  CType type();

  Type VOID = new Primitive(CType.VOID);
  Type INT = new Primitive(CType.INT);
  Type BOOL = new Primitive(CType.BOOL);
  Type CHAR_ARRAY = new Primitive(CType.CHAR_ARRAY);

  record Primitive(CType type) implements Type {
    public Primitive {
      if (type == CType.STRUCT) {
        throw new RuntimeException("should not be reached");
      }
    }

    @Override
    public String print(int indent) {
      return switch (type) {
        case BOOL -> "bool";
        case INT -> "int";
        case VOID -> "void";
        case CHAR_ARRAY -> "char*";
        case STRUCT -> throw new RuntimeException("should not be reached");
      };
    }
  }

  record Struct(String name) implements Type {
    @Override
    public CType type() {
      return CType.STRUCT;
    }

    @Override
    public String print(int indent) {
      return "struct " + name + "*";
    }
  }
}
