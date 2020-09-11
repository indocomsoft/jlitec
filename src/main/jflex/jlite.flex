package jlitec;

import java_cup.runtime.Symbol;
import jlitec.sym;

/** Lexer for JLite **/

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

BooleanLiteral = "true" | "false"

Comment = {MultiLineComment} | {EndOfLineComment}
MultiLineComment = "/*" ~"*/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}

%state STRING

%%

<YYINITIAL> {
  {Identifier} { return symbol(sym.ID); }
}
