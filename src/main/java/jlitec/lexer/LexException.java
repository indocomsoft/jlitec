package jlitec.lexer;

/** LexException is thrown when there is an error during lexing. */
public class LexException extends RuntimeException {
  public final String message;
  public final int line;
  public final int column;
  public final int length;

  /**
   * The only constructor.
   *
   * @param message message.
   * @param line line number.
   * @param column column number.
   * @param length length of the token.
   */
  public LexException(String message, int line, int column, int length) {
    super(String.format("(%d:%d, %d) %s", line, column, length, message));
    this.message = message;
    this.line = line;
    this.column = column;
    this.length = length;
  }
}
