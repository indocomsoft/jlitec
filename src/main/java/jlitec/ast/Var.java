package jlitec.ast;

public class Var {
  public final Type type;
  public final String id;

  public Var(Type type, String id) {
    this.type = type;
    this.id = id;
  }
}
