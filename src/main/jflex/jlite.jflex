package jlitec.generated;

import jlitec.generated.Sym;
import jlitec.lexer.LexException;
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
%implements Sym


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
  "class" { return symbol(CLASS); }
  "main" { return symbol(MAIN); }
  "if" { return symbol(IF); }
  "else" { return symbol(ELSE); }
  "while" { return symbol(WHILE); }
  "readln" { return symbol(READLN); }
  "println" { return symbol(PRINTLN); }
  "return" { return symbol(RETURN); }
  "this" { return symbol(THIS); }
  "new" { return symbol(NEW); }
  "null" { return symbol(NULL); }

  /* Types */
  "Int" { return symbol(INT); }
  "Bool" { return symbol(BOOL); }
  "String" { return symbol(STRING); }
  "Void" { return symbol(VOID); }

  /* Punctuations */
  "{" { return symbol(LBRACE); }
  "}" { return symbol(RBRACE); }
  "(" { return symbol(LPAREN); }
  ")" { return symbol(RPAREN); }
  ";" { return symbol(SEMICOLON); }
  "," { return symbol(COMMA); }
  "." { return symbol(DOT); }

  /* Operators */
  "=" { return symbol(ASSIGN); }
  "||" { return symbol(OR); }
  "&&" { return symbol(AND); }
  ">" { return symbol(GT); }
  "<" { return symbol(LT); }
  ">=" { return symbol(GEQ); }
  "<=" { return symbol(LEQ); }
  "==" { return symbol(EQ); }
  "!=" { return symbol(NEQ); }
  "!" { return symbol(NOT); }
  "+" { return symbol(PLUS); }
  "-" { return symbol(MINUS); }
  "*" { return symbol(MULT); }
  "/" { return symbol(DIV); }

  /* Booleans */
  "true" { return symbol(TRUE); }
  "false" { return symbol(FALSE); }

  {Identifier} { return symbol(ID, yytext()); }
  {ClassName} { return symbol(CNAME, yytext()); }
  {IntegerLiteral} { return symbol(INTEGER_LITERAL, Integer.valueOf(Integer.parseInt(yytext()))); }

  {Comment} { /* Ignore */ }
  {WhiteSpace} { /* Ignore */ }
}

<STRING> {
  \" { yybegin(YYINITIAL); return symbol(STRING_LITERAL, string.toString()); }
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
    if (intVal > 255) throw new LexException(String.format("Decimal value is outside ASCII range: %s", yytext()), yyline, yycolumn);
    char val = (char) intVal;
    string.append(val);
  }

  \\. { throw new LexException(String.format("Illegal escape sequence \"%s\"", yytext()), yyline, yycolumn); }
  {LineTerminator} { throw new LexException("Unterminated string at end of line", yyline, yycolumn); }
}

<<EOF>> { return symbol(EOF); }
