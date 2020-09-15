package jlitec.command;

import net.sourceforge.argparse4j.inf.Namespace;

public enum CommandType {
  LEXER(new LexerCommand());

  private Command command;

  CommandType(Command command) {
    this.command = command;
  }

  /**
   * Runs a specified command based on the given parsed arguments.
   *
   * @param parsed The parsed argument
   */
  public static void run(Namespace parsed) {
    CommandType.valueOf(parsed.getString("subcommand").toUpperCase()).command.run(parsed);
  }
}
