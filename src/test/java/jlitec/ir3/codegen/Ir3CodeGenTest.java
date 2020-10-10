package jlitec.ir3.codegen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.parser.ParserWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class Ir3CodeGenTest {
  @ParameterizedTest
  @MethodSource("loadTests")
  void test(String filename) throws Exception {
    final var program = new ParserWrapper(filename).parse();
    final var klassDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
    final var astProgram = ParseTreeStaticChecker.toAst(program, klassDescriptorMap);
    assertDoesNotThrow(() -> Ir3CodeGen.generate(astProgram).print(0));
  }

  private static Stream<String> loadTests() throws IOException {
    return Files.list(Paths.get("test", "ir3")).map(f -> f.toAbsolutePath().toString());
  }
}
