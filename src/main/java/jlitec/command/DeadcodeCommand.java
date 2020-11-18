package jlitec.command;

import java.io.IOException;
import java.util.Map;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.backend.passes.optimization.deadcode.DeadcodeOptimizationPass;
import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class DeadcodeCommand implements Command {
  @Override
  public String helpMessage() {
    return "Runs deadcode optimization pass and print flow graphs in DOT format";
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
    final var optimizedProgram = new DeadcodeOptimizationPass().pass(lowerProgram);
    for (final var method : optimizedProgram.methodList()) {
      final var flow = new FlowPass().pass(method.lowerStmtList());
      System.out.println("method.id() = " + method.id());
      System.out.println(flow.generateDot());
      System.out.println("---");
      System.out.println(method.print(0));
      System.out.println("======");
    }
  }
}
