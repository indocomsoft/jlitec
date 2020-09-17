package jlitec.parser;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import java_cup.runtime.ComplexSymbolFactory;
import jlitec.ast.Program;
import jlitec.generated.Lexer;
import jlitec.generated.parser;
import jlitec.lexer.LexException;

public class ParserWrapper {
  private static final int PAD = 2;
  private static final Map<String, String> FRIENDLY_SYMBOL_ID =
      Map.ofEntries(
          Map.entry("CLASS", "class"),
          Map.entry("MAIN", "main"),
          Map.entry("IF", "if"),
          Map.entry("ELSE", "else"),
          Map.entry("WHILE", "while"),
          Map.entry("READLN", "readln"),
          Map.entry("PRINTLN", "println"),
          Map.entry("RETURN", "return"),
          Map.entry("THIS", "this"),
          Map.entry("NEW", "new"),
          Map.entry("NULL", "null"),
          Map.entry("INT", "Int"),
          Map.entry("BOOL", "Bool"),
          Map.entry("STRING", "String"),
          Map.entry("VOID", "Void"),
          Map.entry("LBRACE", "{"),
          Map.entry("RBRACE", "}"),
          Map.entry("LPAREN", "("),
          Map.entry("RPAREN", ")"),
          Map.entry("SEMICOLON", ";"),
          Map.entry("COMMA", ","),
          Map.entry("DOT", "."),
          Map.entry("ASSIGN", "="),
          Map.entry("OR", "||"),
          Map.entry("AND", "&&"),
          Map.entry("GT", ">"),
          Map.entry("LT", "<"),
          Map.entry("GEQ", ">="),
          Map.entry("LEQ", "<="),
          Map.entry("EQ", "=="),
          Map.entry("NEQ", "!="),
          Map.entry("NOT", "!"),
          Map.entry("PLUS", "+"),
          Map.entry("MINUS", "-"),
          Map.entry("MULT", "*"),
          Map.entry("DIV", "/"),
          Map.entry("TRUE", "true"),
          Map.entry("FALSE", "false"),
          Map.entry("ID", "identifier"),
          Map.entry("CNAME", "Cname"));

  private final List<String> lines;
  private final String filename;

  public ParserWrapper(String filename) throws IOException {
    this.filename = filename;
    this.lines = Files.readAllLines(Paths.get(filename));
  }

  /**
   * Parse the file.
   *
   * @return the Program AST.
   */
  public Program parse() throws Exception {
    try {
      return (Program)
          new parser(new Lexer(new StringReader(String.join("\n", lines))), this).parse().value;
    } catch (LexException e) {
      System.err.println(String.format(" --> %s:%d:%d", this.filename, e.line, e.column));
      System.err.println("lex error:");
      System.err.println(e.message);
      System.err.println(formErrorString(e.line, e.column, e.length, e.message));
      throw e;
    }
  }

  public void handleCUPError(String message, ComplexSymbolFactory.ComplexSymbol cs) {
    // Skip the end of parse message
    if (message.equals("Couldn't repair and continue parse")) {
      return;
    }
    final var sb = new StringBuilder();
    final var left = cs.xleft;
    final var right = cs.xright;
    sb.append(" --> ").append(this.filename)
            .append(':')
        .append(left.getLine() + 1)
        .append(':')
        .append(left.getColumn() + 1)
        .append("\nParse error: \n")
        .append(message);
    sb.append("\n");
    sb.append(
        formErrorString(left.getLine(), left.getColumn(), right.getColumn() - left.getColumn(), message));
    System.err.println(sb.toString());
  }

  public void handleSyntaxError(
      ComplexSymbolFactory.ComplexSymbol curToken, List<String> expectedTokens) {
    handleCUPError("Syntax error", curToken);
    final var translatedTokens =
        expectedTokens.stream()
            .map(token -> FRIENDLY_SYMBOL_ID.getOrDefault(token, token))
            .collect(ImmutableList.toImmutableList());
    System.err.println("instead expected token classes are " + translatedTokens);
    System.err.println("Note: not all token classes will end up being accepted.");
    System.err.println(
        "The JLite grammar is more restrictive than the LALR parser used, so we perform some parse tree checks later on.");
    System.err.println();
  }

  private String formErrorString(int lineNumber, int column, int length, String message) {
    final var sb = new StringBuilder();

    final int start = Math.max(lineNumber - PAD, 0);
    final int end = Math.min(lineNumber + PAD + 1, this.lines.size());

    final int lineNumberLength = Integer.toString(end).length();
    final var formatString = String.format("%%%dd | %%s\n", lineNumberLength);

    IntStream.range(start, lineNumber).forEachOrdered(num -> sb.append(String.format(formatString, num + 1, lines.get(num))).append(" ".repeat(lineNumberLength)).append(" |\n"));
    sb.append(String.format(formatString, lineNumber + 1, lines.get(lineNumber)));
    sb.append(" ".repeat(lineNumberLength)).append(" | ");
    if (column >= 0) {
      sb.append("~".repeat(column)).append("^".repeat(Math.max(length, 1)));
    } else {
      sb.append("~".repeat(lines.get(lineNumber).length()));
    }
    sb.append(" ").append(message);
    sb.append("\n");
    IntStream.range(lineNumber + 1, end).forEachOrdered(num -> sb.append(String.format(formatString, num + 1, lines.get(num))).append(" ".repeat(lineNumberLength)).append(" |\n"));

    return sb.toString();
  }
}
