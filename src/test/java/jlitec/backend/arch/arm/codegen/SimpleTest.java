package jlitec.backend.arch.arm.codegen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import jlitec.checker.ParseTreeStaticChecker;
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
          Global.gen(ir3Program);
          Simple.gen(ir3Program);
        });
  }

  private static Stream<String> loadTests() throws IOException {
    return Files.list(Paths.get("test", "arm")).map(f -> f.toAbsolutePath().toString());
  }
}
