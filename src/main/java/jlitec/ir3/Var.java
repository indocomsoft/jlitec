package jlitec.ir3;

public record Var(Type type, String id) {
  public Var(jlitec.ast.Var var) {
    this(Type.fromAst(var.type()), var.id());
  }
}
