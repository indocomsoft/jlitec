package jlitec.generated;

import jlitec.generated.sym;
import jlitec.lexer.LexException;
import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

/* Lexer for JLite */

%%

%final
%public
%class Lexer
%unicode
%cup
%cupdebug
%line
%column


%{
  StringBuilder string = new StringBuilder();
  ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();

  private Symbol symbol(String name, int sym) {
      Location left = new Location(yyline, yycolumn, (int)yychar);
      Location right = new Location(yyline, yycolumn + yylength());
      return symbolFactory.newSymbol(name, sym, left, right);
  }

  private Symbol symbol(String name, int sym, Object val) {
      Location left = new Location(yyline, yycolumn, (int)yychar);
      Location right = new Location(yyline, yycolumn + yylength());
      return symbolFactory.newSymbol(name, sym, left, right, val);
  }

  private Symbol symbol(String name, int sym, Object val, int buflength) {
      Location left = new Location(yyline, yycolumn - buflength - 1);
      Location right = new Location(yyline, yycolumn + yylength());
      return symbolFactory.newSymbol(name, sym, left, right, val);
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Identifier = [a-z][a-zA-Z0-9_]*

ClassName = [A-Z][a-zA-Z0-9]*

HexDigit = [0-9a-fA-F]
DecDigit = [0-9]

IntegerLiteral = {DecDigit}+

Comment = {MultiLineComment} | {EndOfLineComment}
MultiLineComment = "/*" ~"*/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}


%state STRING

%%

<YYINITIAL> {
  /* invalid empty string literal */
  // Supposed to use `null` instead
  \"\" { throw new LexException("Illegal empty quoted string literal. Empty string is represented by null", yyline, yycolumn, yylength()); }

  /* string literal */
  \" { yybegin(STRING); string.setLength(0); }

  /* Keywords */
  "class" { return symbol("class", sym.CLASS); }
  "main" { return symbol("main", sym.MAIN); }
  "if" { return symbol("if", sym.IF); }
  "else" { return symbol("else", sym.ELSE); }
  "while" { return symbol("while", sym.WHILE); }
  "readln" { return symbol("readln", sym.READLN); }
  "println" { return symbol("println", sym.PRINTLN); }
  "return" { return symbol("return", sym.RETURN); }
  "this" { return symbol("this", sym.THIS); }
  "new" { return symbol("new", sym.NEW); }
  "null" { return symbol("null", sym.NULL); }

  /* Types */
  "Int" { return symbol("Int", sym.INT); }
  "Bool" { return symbol("Bool", sym.BOOL); }
  "String" { return symbol("String", sym.STRING); }
  "Void" { return symbol("Void", sym.VOID); }

  /* Punctuations */
  "{" { return symbol("{", sym.LBRACE); }
  "}" { return symbol("}", sym.RBRACE); }
  "(" { return symbol("(", sym.LPAREN); }
  ")" { return symbol(")", sym.RPAREN); }
  ";" { return symbol(";", sym.SEMICOLON); }
  "," { return symbol(",", sym.COMMA); }
  "." { return symbol(".", sym.DOT); }

  /* Operators */
  "=" { return symbol("=", sym.ASSIGN); }
  "||" { return symbol("||", sym.OR); }
  "&&" { return symbol("&&", sym.AND); }
  ">" { return symbol(">", sym.GT); }
  "<" { return symbol("<", sym.LT); }
  ">=" { return symbol(">=", sym.GEQ); }
  "<=" { return symbol("<=", sym.LEQ); }
  "==" { return symbol("==", sym.EQ); }
  "!=" { return symbol("!=", sym.NEQ); }
  "!" { return symbol("!", sym.NOT); }
  "+" { return symbol("+", sym.PLUS); }
  "-" { return symbol("-", sym.MINUS); }
  "*" { return symbol("*", sym.MULT); }
  "/" { return symbol("/", sym.DIV); }

  /* Booleans */
  "true" { return symbol("true", sym.TRUE); }
  "false" { return symbol("false", sym.FALSE); }

  {Identifier} { return symbol("identifier", sym.ID, yytext()); }
  {ClassName} { return symbol("cname", sym.CNAME, yytext()); }
  {IntegerLiteral} { return symbol("INTEGER_LITERAL", sym.INTEGER_LITERAL, Integer.valueOf(Integer.parseInt(yytext()))); }

  {Comment} { /* Ignore */ }
  {WhiteSpace} { /* Ignore */ }
}

<STRING> {
  \" { yybegin(YYINITIAL); return symbol("STRING_LITERAL", sym.STRING_LITERAL, string.toString(), string.length()); }
  [^\n\r\"\\] { string.append(yytext()); }

  /* escape sequences */
  "\\\\" { string.append('\\'); }
  "\\\"" { string.append('\"'); }
  "\\n" { string.append('\n'); }
  "\\r" { string.append('\r'); }
  "\\t" { string.append('\t'); }
  "\\b" { string.append('\b'); }
  \\x{HexDigit}?{HexDigit} { char val = (char) Integer.parseInt(yytext().substring(2), 16); string.append(val); }
  \\[0-2]?{DecDigit}?{DecDigit} {
    int intVal = Integer.parseInt(yytext().substring(1));
    if (intVal > 255) throw new LexException(String.format("Decimal value is outside ASCII range: %s", yytext()), yyline, yycolumn, yylength() - this.string.length());
    char val = (char) intVal;
    string.append(val);
  }

  \\. { throw new LexException(String.format("Illegal escape sequence \"%s\"", yytext()), yyline, yycolumn, yylength() - this.string.length()); }
  {LineTerminator} { throw new LexException("Unterminated string at end of line", yyline, yycolumn, yylength()); }
}

/* error fallback */
[^] { throw new LexException("Illegal character \"" + yytext() + "\"", yyline, yycolumn, yylength()); }
<<EOF>> {return symbol("EOF",sym.EOF); }
