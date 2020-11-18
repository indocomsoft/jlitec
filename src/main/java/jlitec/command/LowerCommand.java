package jlitec.command;

import java.io.IOException;
import java.util.Map;
import jlitec.backend.passes.PassManager;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class LowerCommand implements Command {
  @Override
  public String helpMessage() {
    return "Runs the lower pass";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
    subparser.addArgument("--opt").action(Arguments.storeTrue()).help("Run optimization pass");
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
    final jlitec.ast.Program astProgram;
    try {
      classDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
      astProgram = ParseTreeStaticChecker.toAst(program, classDescriptorMap);
    } catch (SemanticException e) {
      System.err.println(ParseTreeStaticChecker.generateErrorMessage(e, parser));
      return;
    }

    final var opt = parsed.getBoolean("opt");
    final jlitec.ir3.Program ir3Program = Ir3CodeGen.generate(astProgram);
    final var output = new LowerPass().pass(ir3Program);
    if (opt) {
      final var optOutput = PassManager.performOptimizationPasses(output);
      System.out.println(optOutput.print(0));
    } else {
      System.out.println(output.print(0));
    }
  }
}
