package jlitec.ast;

/** Represents the possible types in JLite. */
public enum JliteType {
  INT("Int"),
  BOOL("Bool"),
  STRING("String"),
  VOID("Void"),
  CLASS(null); // Intentionally set this to null to crash on printing.

  private String representation;

  JliteType(String representation) {
    this.representation = representation;
  }

  @Override
  public String toString() {
    assert representation != null;
    return representation;
  }
}
