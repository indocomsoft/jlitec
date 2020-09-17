package jlitec.ast;

public record Type(String cname, JliteType type) implements Printable {
  public Type {
    assert (cname == null && type == JliteType.CLASS) || (cname != null && type != JliteType.CLASS);
  }

  /**
   * Constructor for types based on a class name.
   *
   * @param cname The name of the class as the type.
   */
  public Type(String cname) {
    this(cname, JliteType.CLASS);
  }

  /**
   * Constructor for primitive types.
   *
   * @param type A primitive JLiteType.
   */
  public Type(JliteType type) {
    this(null, type);
  }

  @Override
  public String print(int indent) {
    return this.cname == null ? this.type.toString() : this.cname;
  }
}
