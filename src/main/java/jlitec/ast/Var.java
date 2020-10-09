package jlitec.ast;

public record Var(Type type, String id) implements TypeAnnotable {
  public Var(jlitec.parsetree.Var var) {
    this(Type.fromParseTree(var.type()), var.name().id());
  }

  @Override
  public TypeAnnotation typeAnnotation() {
    return switch (type.jliteType()) {
      case BOOL -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.BOOL);
      case STRING -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.STRING);
      case VOID -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.VOID);
      case CLASS -> new TypeAnnotation.Klass(((KlassType) type).cname());
      case INT -> new TypeAnnotation.Primitive(TypeAnnotation.Annotation.INT);
    };
  }
}
