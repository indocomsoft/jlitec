package jlitec.parser;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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

  public ParserWrapper(String filename) throws IOException {
    this.lines = Files.readAllLines(Paths.get(filename));
  }

  /**
   * Parse the file.
   *
   * @return the Program AST.
   */
  public Program parse() {
    try {
      return (Program)
          new parser(new Lexer(new StringReader(String.join("\n", lines))), this).parse().value;
    } catch (LexException e) {
      System.err.println("Lexing error:");
      System.err.println(e.getLocalizedMessage());
      System.err.println();
      System.err.println(formErrorString(e.line, e.column, e.length));
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void handleCUPError(String message, ComplexSymbolFactory.ComplexSymbol cs) {
    final var sb = new StringBuilder();
    final var left = cs.xleft;
    final var right = cs.xright;
    sb.append('(')
        .append(left.getLine() + 1)
        .append(':')
        .append(left.getColumn() + 1)
        .append(") parse error:\n")
        .append(message);
    sb.append("\n");
    sb.append(
        formErrorString(left.getLine(), left.getColumn(), right.getColumn() - left.getColumn()));
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
    System.err.println("The JLite grammar is more restrictive than the LALR parser used, so we perform some parse tree checks later on.");
    System.err.println();
  }

  private String formErrorString(int lineNumber, int column, int length) {
    final var sb = new StringBuilder();

    int start = Math.max(lineNumber - PAD, 0);
    int end = Math.min(lineNumber + PAD + 1, this.lines.size());

    lines.subList(start, lineNumber + 1).forEach(line -> sb.append(line).append("\n"));
    if (column >= 0) {
      sb.append("~".repeat(column)).append("^".repeat(Math.max(length, 1)));
    } else {
      sb.append("~".repeat(lines.get(lineNumber).length()));
    }
    sb.append("\n");
    lines.subList(lineNumber + 1, end).forEach(line -> sb.append(line).append("\n"));

    return sb.toString();
  }
}
