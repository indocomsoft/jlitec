package jlitec.checker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ParseTreeStaticCheckerTest {
  @ParameterizedTest
  @MethodSource("loadPassingDistinctNameTests")
  void testPassingDistincNameTests(String filename) throws Exception {
    Program program = new ParserWrapper(filename).parse();
    assertDoesNotThrow(() -> ParseTreeStaticChecker.distinctNameCheck(program));
  }

  @ParameterizedTest
  @MethodSource("loadFailingDistinctNameTests")
  void testFailingTests(String filename) throws Exception {
    Program program = new ParserWrapper(filename).parse();
    assertThrows(SemanticException.class, () -> ParseTreeStaticChecker.distinctNameCheck(program));
  }

  @ParameterizedTest
  @MethodSource("loadPassingTypecheckTests")
  void testPassingTypecheckTests(String filename) throws Exception {
    Program program = new ParserWrapper(filename).parse();
    final var klassDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
    assertDoesNotThrow(() -> ParseTreeStaticChecker.typecheck(program, klassDescriptorMap));
  }

  @ParameterizedTest
  @MethodSource("loadFailingTypecheckTests")
  void testFailingTypecheckTests(String filename) throws Exception {
    Program program = new ParserWrapper(filename).parse();
    final var klassDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
    assertThrows(
        SemanticException.class,
        () -> ParseTreeStaticChecker.typecheck(program, klassDescriptorMap));
  }

  private static Stream<String> loadPassingDistinctNameTests() throws IOException {
    return Files.list(Paths.get("test", "distinctname", "pass"))
        .map(f -> f.toAbsolutePath().toString());
  }

  private static Stream<String> loadFailingDistinctNameTests() throws IOException {
    return Files.list(Paths.get("test", "distinctname", "fail"))
        .map(f -> f.toAbsolutePath().toString());
  }

  private static Stream<String> loadPassingTypecheckTests() throws IOException {
    return Files.list(Paths.get("test", "type", "pass")).map(f -> f.toAbsolutePath().toString());
  }

  private static Stream<String> loadFailingTypecheckTests() throws IOException {
    return Files.list(Paths.get("test", "type", "fail")).map(f -> f.toAbsolutePath().toString());
  }
}
