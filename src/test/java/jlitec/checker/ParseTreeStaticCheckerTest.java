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
  @MethodSource("loadPassingTests")
  void testPassingTests(String filename) throws Exception {
    Program program = new ParserWrapper(filename).parse();
    assertDoesNotThrow(() -> ParseTreeStaticChecker.distinctNameCheck(program));
  }

  @ParameterizedTest
  @MethodSource("loadFailingTests")
  void testFailingTests(String filename) throws Exception {
    Program program = new ParserWrapper(filename).parse();
    assertThrows(SemanticException.class, () -> ParseTreeStaticChecker.distinctNameCheck(program));
  }

  private static Stream<String> loadPassingTests() throws IOException {
    return Files.list(Paths.get("test", "distinctname", "pass"))
        .map(f -> f.toAbsolutePath().toString());
  }

  private static Stream<String> loadFailingTests() throws IOException {
    return Files.list(Paths.get("test", "distinctname", "fail"))
        .map(f -> f.toAbsolutePath().toString());
  }
}
