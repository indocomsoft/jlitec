package jlitec.parsetree;

import jlitec.Printable;

/** Represents the possible types in JLite. */
public enum JliteType implements Printable {
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
  public String print(int indent) {
    assert representation != null;
    return representation;
  }
}
