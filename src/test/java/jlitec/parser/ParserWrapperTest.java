package jlitec.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import jlitec.lexer.LexException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ParserWrapperTest {
  @ParameterizedTest
  @MethodSource("loadPassingTests")
  void testPassingTests(String filename) {
    assertDoesNotThrow(() -> new ParserWrapper(filename).parse());
  }

  @ParameterizedTest
  @MethodSource("loadFailingTests")
  void testFailingTests(String filename) {
    final var expected = filename.contains("lexing") ? LexException.class : Exception.class;
    assertThrows(Exception.class, () -> new ParserWrapper(filename).parse());
  }

  private static Stream<String> loadPassingTests() throws IOException {
    return Files.list(Paths.get("test", "parsing", "pass")).map(f -> f.toAbsolutePath().toString());
  }

  private static Stream<String> loadFailingTests() throws IOException {
    return Files.list(Paths.get("test", "parsing", "fail")).map(f -> f.toAbsolutePath().toString());
  }
}
