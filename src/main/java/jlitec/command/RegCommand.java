package jlitec.command;

import java.io.IOException;
import java.util.Map;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.backend.passes.optimization.PassManager;
import jlitec.backend.passes.regalloc.RegAllocPass;
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

public class RegCommand implements Command {
  @Override
  public String helpMessage() {
    return "Runs the register allocation pass.";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
    subparser
        .addArgument("--opt")
        .help("Run optimization passes first")
        .action(Arguments.storeTrue());
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
    final var lowerProgram = new LowerPass().pass(ir3Program);
    final var finalProgram =
        opt ? PassManager.performOptimizationPasses(lowerProgram) : lowerProgram;
    for (final var method : finalProgram.methodList()) {
      System.out.println(method.id());
      final var output = new RegAllocPass().pass(method);
      System.out.println(output.color());
      System.out.println(output.method().print(0));
      System.out.println("===");
    }
  }
}
