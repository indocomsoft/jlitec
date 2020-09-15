package jlitec.command;

import java.io.IOException;
import jlitec.parser.ParserWrapper;
import jlitec.ast.Program;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class PrettyPrintCommand implements Command {
  @Override
  public String helpMessage() {
    return "pretty print the source code";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
  }

  @Override
  public void run(Namespace parsed) {
    final String filename = parsed.getString("filename");
    try {
      final Program program = new ParserWrapper(filename).parse();
      System.out.println(program.print(0));
    } catch (IOException e) {
      System.err.println("Unable to read file.");
    }
  }
}
