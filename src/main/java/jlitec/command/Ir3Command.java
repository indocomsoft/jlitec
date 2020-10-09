package jlitec.command;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Locatable;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class Ir3Command implements Command {
  @Override
  public String helpMessage() {
    return "Generate IR3 intermediate code";
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

    final Map<String, KlassDescriptor> classDescriptorMap;
    try {
      classDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
      ParseTreeStaticChecker.typecheck(program, classDescriptorMap);
    } catch (SemanticException e) {
      final var lines =
          e.locatableList.stream()
              .map(Locatable::leftLocation)
              .map(l -> "--> " + filename + ":" + (l.getLine() + 1) + ":" + (l.getColumn() + 1))
              .collect(Collectors.joining("\n"));
      System.err.println(lines + "\nSemantic error: " + e.getMessage());
      for (final var locatable : e.locatableList) {
        System.err.println(parser.formErrorString(e.shortMessage, locatable));
      }
      return;
    }

    final jlitec.ast.Program astProgram = ParseTreeStaticChecker.toAst(program, classDescriptorMap);
    final jlitec.ir3.Program ir3Program = Ir3CodeGen.generate(astProgram);
    System.out.println(ir3Program);
    System.out.println(ir3Program.print(0));
  }
}
