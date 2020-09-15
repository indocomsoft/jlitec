package jlitec.command;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class LexerCommand implements Command {
  @Override
  public String helpMessage() {
    return "Show the result of lexing the source code for debugging.";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
  }

  @Override
  public void run(Namespace parsed) {
    jlitec.generated.Lexer.main(new String[] {parsed.get("filename")});
  }
}
