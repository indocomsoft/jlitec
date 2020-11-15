package jlitec.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import jlitec.backend.passes.flow.ProgramWithFlow;
import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import jlitec.backend.passes.flow.FlowPass;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class FlowCommand implements Command {
  @Override
  public String helpMessage() {
    return "Runs the flow pass";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
    subparser.addArgument("--output-dir", "-o").type(String.class).help("output directory").required(true);
  }

  @Override
  public void run(Namespace parsed) {
    final String filename = parsed.getString("filename");
    final String outputDir = parsed.getString("output_dir");

    if (!Files.isDirectory(Paths.get(outputDir))) {
      System.err.println("Output directory " + outputDir + " does not exist.");
      return;
    }

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
    final ProgramWithFlow programWithFlow = new FlowPass().pass(ir3Program);
    for (final var entry : programWithFlow.methodToFlow().entrySet()) {
      final var methodName = entry.getKey().id();
      final var flow = entry.getValue();
      final var path = Paths.get(outputDir, methodName + ".dot");
      try {
        Files.write(path, flow.generateDot().getBytes());
      } catch (IOException e) {
        System.err.println("Unable to write " + path.toString() + ": " + e.getMessage());
      }
    }
  }
}
