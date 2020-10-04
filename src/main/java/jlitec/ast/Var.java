package jlitec.ast;

public record Var(Type type, String id) {
  public Var(jlitec.parsetree.Var var) {
    this(Type.fromParseTree(var.type()), var.name().id());
  }
}
