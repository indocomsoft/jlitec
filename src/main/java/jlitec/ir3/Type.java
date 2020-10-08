package jlitec.ir3;

public interface Type {
  Ir3Type type();

  record PrimitiveType(Ir3Type type) implements Type {}

  record KlassType(String cname) implements Type {
    @Override
    public Ir3Type type() {
      return Ir3Type.CLASS;
    }
  }
}
