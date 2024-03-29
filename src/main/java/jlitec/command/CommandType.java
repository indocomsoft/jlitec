package jlitec.command;

import net.sourceforge.argparse4j.inf.Namespace;

public enum CommandType {
  LEXER(new LexerCommand()),
  PARSE_TREE(new ParseTreeCommand()),
  PRETTY_PRINT(new PrettyPrintCommand()),
  CHECK(new CheckCommand()),
  IR3(new Ir3Command()),
  C(new CCommand()),
  FLOW(new FlowCommand()),
  LIVE(new LiveCommand()),
  INTERFERENCE(new InterferenceCommand()),
  LOWER(new LowerCommand()),
  REG(new RegCommand()),
  ARM(new ArmCommand()),
  DEADCODE(new DeadcodeCommand()),
  ALG(new AlgCommand()),
  SIMPLE(new SimpleCommand());

  public final Command command;

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
