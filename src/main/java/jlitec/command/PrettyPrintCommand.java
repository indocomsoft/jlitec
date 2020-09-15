package jlitec.command;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java_cup.runtime.DefaultSymbolFactory;
import jlitec.ast.Program;
import jlitec.generated.Lexer;
import jlitec.generated.parser;
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
    final Program program;
    try {
      program =
          (Program)
              new parser(
                      new Lexer(new FileReader(filename, StandardCharsets.UTF_8)),
                      new DefaultSymbolFactory())
                  .parse()
                  .value;
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Unable to find file \"%s\"", filename), e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println(program.print(0));
  }
}
