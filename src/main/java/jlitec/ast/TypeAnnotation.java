package jlitec.ast;

public interface TypeAnnotation {
  enum Annotation {
    INT,
    STRING,
    VOID,
    BOOL,
    CLASS,
    NULL,
  }

  Annotation annotation();

  record Primitive(Annotation annotation) implements TypeAnnotation {
    public Primitive {
      if (annotation == Annotation.CLASS || annotation == Annotation.NULL) {
        throw new RuntimeException("invalid annotation");
      }
    }
  }

  record Klass(String cname) implements TypeAnnotation {
    @Override
    public Annotation annotation() {
      return Annotation.CLASS;
    }
  }

  record Null() implements TypeAnnotation {
    @Override
    public Annotation annotation() {
      return Annotation.NULL;
    }
  }
}
