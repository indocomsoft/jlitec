package jlitec.lexer;

import jlitec.parser.sym;
import java_cup.runtime.Symbol;

/* Lexer for JLite */

%%

%final
%public
%class Lexer
%unicode
%cup
%line
%column


%{
  StringBuilder string = new StringBuilder();

  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Identifier = [a-z][a-zA-Z0-9_]*

ClassName = [A-Z][a-zA-Z0-9]*

IntegerLiteral = [0-9]+

Comment = {MultiLineComment} | {EndOfLineComment}
MultiLineComment = "/*" ~"*/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}

HexDigit = [0-9a-fA-F]

%state STRING

%%

<YYINITIAL> {
  /* string literal */
  \" { yybegin(STRING); string.setLength(0); }

  /* Keywords */
  "class" { return symbol(sym.CLASS); }
  "main" { return symbol(sym.MAIN); }
  "if" { return symbol(sym.IF); }
  "else" { return symbol(sym.ELSE); }
  "while" { return symbol(sym.WHILE); }
  "readln" { return symbol(sym.READLN); }
  "println" { return symbol(sym.PRINTLN); }
  "return" { return symbol(sym.RETURN); }
  "this" { return symbol(sym.THIS); }
  "new" { return symbol(sym.NEW); }
  "null" { return symbol(sym.NULL); }

  /* Types */
  "Int" { return symbol(sym.INT); }
  "Bool" { return symbol(sym.BOOL); }
  "String" { return symbol(sym.STRING); }
  "Void" { return symbol(sym.VOID); }

  /* Punctuations */
  "{" { return symbol(sym.LBRACE); }
  "}" { return symbol(sym.RBRACE); }
  "(" { return symbol(sym.LPAREN); }
  ")" { return symbol(sym.RPAREN); }
  ";" { return symbol(sym.SEMICOLON); }
  "," { return symbol(sym.COMMA); }
  "." { return symbol(sym.DOT); }

  /* Operators */
  "=" { return symbol(sym.ASSIGN); }
  "||" { return symbol(sym.OR); }
  "&&" { return symbol(sym.AND); }
  ">" { return symbol(sym.GT); }
  "<" { return symbol(sym.LT); }
  ">=" { return symbol(sym.GEQ); }
  "<=" { return symbol(sym.LEQ); }
  "==" { return symbol(sym.EQ); }
  "!=" { return symbol(sym.NEQ); }
  "!" { return symbol(sym.NOT); }
  "+" { return symbol(sym.PLUS); }
  "-" { return symbol(sym.MINUS); }
  "*" { return symbol(sym.MULT); }
  "/" { return symbol(sym.DIV); }

  /* Booleans */
  "true" { return symbol(sym.TRUE); }
  "false" { return symbol(sym.FALSE); }

  {Identifier} { return symbol(sym.ID, yytext()); }
  {ClassName} { return symbol(sym.CNAME, yytext()); }
  {IntegerLiteral} { return symbol(sym.INTEGER_LITERAL, new Integer(Integer.parseInt(yytext()))); }

  {Comment} { /* Ignore */ }
  {WhiteSpace} { /* Ignore */ }
}

<STRING> {
  \" { yybegin(YYINITIAL); return symbol(sym.STRING_LITERAL, string.toString()); }
  [^\n\r\"\\] { string.append(yytext()); }

  /* escape sequences */
  "\\\\" { string.append('\\'); }
  "\\\"" { string.append('\"'); }
  "\\n" { string.append('\n'); }
  "\\r" { string.append('\r'); }
  "\\t" { string.append('\t'); }
  "\\b" { string.append('\b'); }
  \\x{HexDigit}?{HexDigit} { char val = (char) Integer.parseInt(yytext().substring(2), 16); string.append(val); }
  \\{IntegerLiteral} {
    int intVal = Integer.parseInt(yytext().substring(1));
    if (intVal > 255) throw new RuntimeException("Decimal value is outside ASCII range.");
    char val = (char) intVal;
    string.append(val);
  }

  \\. { throw new RuntimeException("Illegal escape sequence \"" + yytext() + "\""); }
  {LineTerminator} { throw new RuntimeException("Unterminated string at end of line"); }
}
