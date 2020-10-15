package jlitec.command;

import java.io.IOException;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

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

    final Program program;

    try {
      program = parser.parse();
    } catch (LexException e) {
      System.err.println("Lexing failed.");
      return;
    } catch (Exception e) {
      System.err.println("Parsing failed: " + e.getMessage());
      return;
    }

    try {
      final var classDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
      ParseTreeStaticChecker.toAst(program, classDescriptorMap);
      System.err.println("Static check succeeded!");
    } catch (SemanticException e) {
      System.err.println(ParseTreeStaticChecker.generateErrorMessage(e, parser));
    }
  }
}
