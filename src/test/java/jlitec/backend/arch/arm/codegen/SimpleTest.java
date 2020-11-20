package jlitec.backend.arch.arm.codegen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.PassManager;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.backend.passes.optimization.constantfolding.ConstantFoldingOptimizationPass;
import jlitec.backend.passes.regalloc.RegAllocPass;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.command.InterferenceCommand;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.parser.ParserWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SimpleTest {
  @ParameterizedTest
  @MethodSource("loadTests")
  void test(String filename) throws Exception {
    assertDoesNotThrow(
        () -> {
          final var program = new ParserWrapper(filename).parse();
          final var klassDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
          final var astProgram = ParseTreeStaticChecker.toAst(program, klassDescriptorMap);
          final var ir3Program = Ir3CodeGen.generate(astProgram);
          final var lowerProgram = new LowerPass().pass(ir3Program);
          new ConstantFoldingOptimizationPass().pass(lowerProgram).print(0);
          final var armProgram = Global.gen(lowerProgram);
          armProgram.print(0);
          PeepholeOptimizer.pass(armProgram).print(0);
          final var optProgram = PassManager.performOptimizationPasses(lowerProgram);
          final var armOptProgram = Global.gen(optProgram);
          armOptProgram.print(0);
          PeepholeOptimizer.pass(armOptProgram).print(0);
          Simple.gen(ir3Program);
          for (final var method : lowerProgram.methodList()) {
            final var flow = new FlowPass().pass(method.lowerStmtList());
            flow.generateDot();
            final var methodWithLive = new LivePass().pass(new MethodWithFlow(method, flow));
            final var edges =
                new InterferenceCommand()
                    .buildInterferenceGraph(methodWithLive.lowerStmtWithLiveList());
            edges.keySet().forEach(n -> n.print(0));
            edges.values().forEach(n -> n.print(0));
            new RegAllocPass().pass(method).method().print(0);
          }
        });
  }

  private static Stream<String> loadTests() throws IOException {
    return Files.list(Paths.get("test", "arm")).map(f -> f.toAbsolutePath().toString());
  }
}
