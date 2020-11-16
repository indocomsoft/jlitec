package jlitec.command;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.flow.ProgramWithFlow;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class LiveCommand implements Command {
  @Override
  public String helpMessage() {
    return "Runs the live pass and output in DOT format";
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
    final ProgramWithFlow programWithFlow = new FlowPass().pass(lowerProgram);
    for (final var method : programWithFlow.program().methodList()) {
      final var flow = programWithFlow.methodToFlow().get(method);
      final var output = new LivePass().pass(new MethodWithFlow(method, flow));
      final var prefix =
          IntStream.range(0, flow.blocks().size())
              .boxed()
              .collect(
                  Collectors.toUnmodifiableMap(
                      Function.identity(),
                      i -> "liveIn = " + output.blockWithLiveList().get(i).liveIn()));
      final var suffix =
          IntStream.range(0, flow.blocks().size())
              .boxed()
              .collect(
                  Collectors.toUnmodifiableMap(
                      Function.identity(),
                      i -> "liveOut = " + output.blockWithLiveList().get(i).liveOut()));
      System.out.println(flow.generateDot(prefix, suffix));
    }
  }
}
