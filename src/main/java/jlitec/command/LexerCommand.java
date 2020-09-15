package jlitec.command;

import net.sourceforge.argparse4j.inf.Namespace;

public class LexerCommand implements Command {
  @Override
  public void run(Namespace parsed) {
    jlitec.generated.Lexer.main(new String[] {parsed.get("filename")});
  }
}
