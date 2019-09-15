package jlite.lexer;

import java_cup.runtime.*;
import jlite.parser.sym;

%%
%public
%class Scanner
%cup
%cupdebug
%unicode
%line
%column

%{
  StringBuilder string = new StringBuilder();
  private ComplexSymbolFactory symbolFactory;

  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }
%}

LineTerminator = \r|\n|\r\n
  InputCharacter = [^\r\n]
  WhiteSpace     = {LineTerminator} | [ \t\f]


/* comments */
  Comment = {TraditionalComment} | {EndOfLineComment}

TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment = "//" {InputCharacter}* {LineTerminator}?

Identifier = [a-z][a-zA-Z0-9_]*
ClassName = [A-Z][a-zA-Z0-9_]*
Integer = [0-9]+

%state STRING

%%
<YYINITIAL> {
  /* keywords */
  "if" { return symbol(sym.IF); }
  "else" { return symbol(sym.ELSE); }
  "class" { return symbol(sym.CLASS); }
  "while" { return symbol(sym.WHILE); }
  "realdln" { return symbol(sym.READLN); }
  "println" { return symbol(sym.PRINTLN); }
  "return" { return symbol(sym.RETURN); }
  "this" { return symbol(sym.THIS); }
  "new" { return symbol(sym.NEW);}
  "main" { return symbol(sym.MAIN); }
  "Void" { return symbol(sym.VOID); }
  "Int" { return symbol(sym.INT); }
  "Bool" { return symbol(sym.BOOL); }
  "String" { return symbol(sym.STRING); }

  /* boolean literals */
  "true" { return symbol(sym.TRUE); }
  "false" { return symbol(sym.FALSE); }
  "null" { return symbol(sym.NULL); }

  /* separators */
  "(" { return symbol(sym.LPAREN); }
  ")" { return symbol(sym.RPAREN); }
  "{" { return symbol(sym.LBRACE); }
  "}" { return symbol(sym.RBRACE); }
  ";" { return symbol(sym.SEMI); }
  "." { return symbol(sym.DOT); }
  "," { return symbol(sym.COMMA); }

  /* operators */
  "+" { return symbol(sym.PLUS); }
  "-" { return symbol(sym.MINUS); }
  "*" { return symbol(sym.MULT); }
  "/" { return symbol(sym.DIV); }
  "<" { return symbol(sym.LT); }
  ">" { return symbol(sym.GT); }
  "<=" { return symbol(sym.LEQ); }
  ">=" { return symbol(sym.GEQ); }
  "==" { return symbol(sym.EQ); }
  "!=" { return symbol(sym.NEQ); }
  "=" { return symbol(sym.ASSIGN); }
  "!" { return symbol(sym.NOT); }
  "||" { return symbol(sym.OR); }
  "&&" { return symbol(sym.AND); }

  /* comments */
  {Comment} { /* ignore */ }

  /* whitespace */
  {WhiteSpace} { /* ignore */ }

  /* literals */
  {Integer} { return symbol(sym.INTEGER_LITERAL, new Integer(Integer.parseInt(yytext()))); }
  \" { yybegin(STRING); string.setLength(0); }

  {Identifier} { return symbol(sym.IDENTIFIER, yytext()); }
  {ClassName} {return symbol(sym.CNAME, yytext()); }
}

<STRING> {
  \" { yybegin(YYINITIAL); return symbol(sym.STRING_LITERAL, string.toString()); }
  [^\n\r\"\\]+                   { string.append( yytext() ); }
  "\\t"                            { string.append('\t'); }
  "\\n"                            { string.append('\n'); }

  "\\r"                            { string.append('\r'); }
  "\\b"                            { string.append('\b'); }
  "\\\""                           { string.append('\"'); }
  "\\\\"                           { string.append('\\'); }
  \\[0-3]?[0-7]?[0-7]            { char val = (char) Integer.parseInt(yytext().substring(1), 8); string.append(val); }
  \\x[0-9a-f]?[0-9a-f]           { char val = (char) Integer.parseInt(yytext().substring(2), 16); string.append(val); }

  /* error cases */
  \\. { throw new LexException("Illegal escape sequence \"" + yytext() + "\"", yyline, yycolumn); }
  {LineTerminator} { throw new LexException("Unterminated string at end of line", yyline, yycolumn); }
}

/* error fallback */
[^] { throw new LexException("Illegal character \"" + yytext() + "\"", yyline, yycolumn); }
<<EOF>> {return symbol(sym.EOF); }
