package jlitec.command;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import jlitec.backend.arch.arm.codegen.Global;
import jlitec.backend.arch.arm.codegen.PeepholeOptimizer;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.backend.passes.optimization.PassManager;
import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ArmCommand implements Command {
  @Override
  public String helpMessage() {
    return "Compile JLite code to ARM assembly.";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
    subparser.addArgument("-O").type(Integer.class).help("optimization level");
  }

  @Override
  public void run(Namespace parsed) {
    final var optLevel = Optional.ofNullable(parsed.getInt("O")).orElse(0);
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

    final jlitec.ir3.Program ir3Program = Ir3CodeGen.generate(astProgram);
    final var lowerProgram = new LowerPass().pass(ir3Program);

    if (optLevel >= 3) {
      final var finalProgram = PassManager.performOptimizationPasses(lowerProgram);
      final var armProgram = Global.gen(finalProgram);
      final var peepholeOptimized = PeepholeOptimizer.pass(armProgram);
      System.out.println(peepholeOptimized.print(0));
    } else if (optLevel > 0) {
      final var finalProgram = PassManager.performOptimizationPasses(lowerProgram);
      final var armProgram = Global.gen(finalProgram);
      System.out.println(armProgram.print(0));
    } else {
      final var armProgram = Global.gen(lowerProgram);
      System.out.println(armProgram.print(0));
    }
  }
}
