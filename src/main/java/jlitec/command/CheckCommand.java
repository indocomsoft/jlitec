package jlitec.command;

import jlitec.ast.Program;
import jlitec.checker.SemanticException;
import jlitec.checker.StaticChecker;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.IOException;

public class CheckCommand implements Command {
  @Override
  public String helpMessage() {
    return "Perform static semantic check";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
  }

  @Override
  public void run(Namespace parsed) {
    final String filename = parsed.getString("filename");
    final ParserWrapper parser;
    try {
      parser = new ParserWrapper(filename);
    } catch (IOException e) {
      System.err.println("Unable to read file.");
      return;
    }

    try {
      final Program program = parser.parse();
      System.out.println(StaticChecker.produceClassDescriptor(program));
    } catch (SemanticException e) {
      System.err.println("Semantic error: " + e.getMessage());
      for (final var locatable : e.locatableList) {
        System.err.println(parser.formErrorString(e.shortMessage, locatable));
      }
    } catch (LexException e) {
      System.err.println("Lexing failed.");
    } catch (Exception e) {
      System.err.println("Parsing failed: " + e.getMessage());
    }
  }
}
