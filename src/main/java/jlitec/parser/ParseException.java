package jlitec.parser;

/** ParseException is thrown when there is an error during parsing. */
public class ParseException extends RuntimeException {
  public final String message;
  public final int line;
  public final int column;

  /**
   * The only constructor.
   *
   * @param message message.
   * @param line line number.
   * @param column column number.
   */
  public ParseException(String message, int line, int column) {
    super(String.format("(%d:%d) %s", line, column, message));
    this.message = message;
    this.line = line;
    this.column = column;
  }
}
