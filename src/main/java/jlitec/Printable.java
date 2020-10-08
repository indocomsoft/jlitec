package jlitec;

public interface Printable {
  int SPACE_INDENT = 2;

  String print(int indent);

  default void indent(StringBuilder sb, int indent) {
    sb.append(" ".repeat(indent * SPACE_INDENT));
  }
}
