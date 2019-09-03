package jlite.lexer;

import java_cup.runtime.Symbol;
import java_cup.runtime.Factory;

%%
%class Lexer
%implements sym
%cup
%unicode
%line
%column

%{
  StringBuffer string = new StringBuffer();
  private ComplexSymbolFactory symbolFactory;

  public Lexer(java.io.Reader in, ComplexSymbolFactory sf) {
    this(in);
    symbolFactory = sf;
  }

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

Identifier = [a-z][a-zA-Z_]*
ClassName = [A-Z][a-zA-Z_]*

%state STRING

%%
/* keywords */
<YYINITIAL> "true" { return symbol(sym.TRUE); }
<YYINITIAL> "false" { return symbol(sym.FALSE); }

<YYINITIAL> {
  /* Identifier */
  {Identifier} { return symbol(sym.IDENTIFIER); }
}
