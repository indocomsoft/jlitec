package jlitec;

import java_cup.runtime.DefaultSymbolFactory;
import jlitec.ast.Program;
import jlitec.generated.Lexer;
import jlitec.generated.parser;
import jlitec.lexer.LexException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ParserWrapper {
  private static final int PAD = 2;

  private final String filename;
  private final List<String> lines;

  public ParserWrapper(String filename) throws IOException {
    this.filename = filename;
    this.lines = Files.readAllLines(Paths.get(filename));
  }

  public Program parse() {
    try {
      return (Program)
              new parser(
                      new Lexer(new StringReader(String.join("\n", lines))),
                      new DefaultSymbolFactory())
                      .parse()
                      .value;
    } catch (LexException e) {
      System.err.println("Lexing error:");
      System.err.println(e.getLocalizedMessage());
      System.err.println();
      System.err.println(formErrorString(e.line, e.column));
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String formErrorString(int lineNumber, int column) {
    final var sb = new StringBuilder();

    int start = Math.max(lineNumber - PAD, 0);
    int end = Math.min(lineNumber + PAD + 1, this.lines.size());

    lines.subList(start, lineNumber + 1).forEach(line -> sb.append(line).append("\n"));
    sb.append("~".repeat(column)).append("^\n");
    lines.subList(lineNumber + 1, end).forEach(line -> sb.append(line).append("\n"));

    return sb.toString();
  }
}
