package jlitec.checker;

import jlitec.parsetree.JliteType;

public interface Type {
  record Basic(String type) implements Type {
    public static Basic NULL = new Basic("null");

    public Basic(JliteType type) {
      this(type.print(0));
    }
  }
}
