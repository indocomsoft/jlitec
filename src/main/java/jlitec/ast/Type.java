package jlitec.ast;

public class Type {
  public final String cname;
  public final JliteType type;

  /**
   * Constructor for types based on a class name.
   *
   * @param cname The name of the class as the type.
   */
  public Type(String cname) {
    this.cname = cname;
    this.type = JliteType.CLASS;
  }

  /**
   * Constructor for primitive types.
   *
   * @param type A primitive JLiteType.
   */
  public Type(JliteType type) {
    assert type != JliteType.CLASS;
    this.cname = null;
    this.type = type;
  }
}
