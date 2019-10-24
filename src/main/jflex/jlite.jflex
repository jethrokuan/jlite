package jlite.lexer;

import jlite.parser.sym;
import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

%%
%public
%class Scanner
%cup
%unicode
%line
%column

%{
  StringBuilder string = new StringBuilder();

  public Scanner(java.io.Reader in, ComplexSymbolFactory sf){
    this(in);
	symbolFactory = sf;
  }

  ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();

  private Symbol symbol(String name, int sym) {
        return symbolFactory.newSymbol(name, sym, new Location(yyline+1,yycolumn+1,yychar), new Location(yyline+1,yycolumn+yylength(),yychar+yylength()));
  }

  private Symbol symbol(String name, int sym, Object val) {
        Location left = new Location(yyline+1,yycolumn+1,yychar);
        Location right= new Location(yyline+1,yycolumn+yylength(), yychar+yylength());
        return symbolFactory.newSymbol(name, sym, left, right,val);
  }

  private Symbol symbol(String name, int sym, Object val,int buflength) {
        Location left = new Location(yyline+1,yycolumn+yylength()-buflength,yychar+yylength()-buflength);
        Location right= new Location(yyline+1,yycolumn+yylength(), yychar+yylength());
        return symbolFactory.newSymbol(name, sym, left, right,val);
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
  "if" { return symbol("if",sym.IF); }
  "else" { return symbol("else",sym.ELSE); }
  "class" { return symbol("class",sym.CLASS); }
  "while" { return symbol("while",sym.WHILE); }
  "realdln" { return symbol("readln",sym.READLN); }
  "println" { return symbol("println",sym.PRINTLN); }
  "return" { return symbol("return",sym.RETURN); }
  "this" { return symbol("this",sym.THIS); }
  "new" { return symbol("new",sym.NEW);}
  "main" { return symbol("main",sym.MAIN); }
  "Void" { return symbol("void",sym.VOID); }
  "Int" { return symbol("int",sym.INT); }
  "Bool" { return symbol("bool",sym.BOOL); }
  "String" { return symbol("string",sym.STRING); }

  /* boolean literals */
  "true" { return symbol("true",sym.TRUE); }
  "false" { return symbol("false",sym.FALSE); }
  "null" { return symbol("null",sym.NULL); }

  /* separators */
  "(" { return symbol("(",sym.LPAREN); }
  ")" { return symbol(")",sym.RPAREN); }
  "{" { return symbol("{",sym.LBRACE); }
  "}" { return symbol("}",sym.RBRACE); }
  ";" { return symbol(";",sym.SEMI); }
  "." { return symbol(".",sym.DOT); }
  "," { return symbol(",",sym.COMMA); }

  /* operators */
  "+" { return symbol("plus",sym.PLUS); }
  "-" { return symbol("minus",sym.MINUS); }
  "*" { return symbol("mult",sym.MULT); }
  "/" { return symbol("div",sym.DIV); }
  "<" { return symbol("lt",sym.LT); }
  ">" { return symbol("gt",sym.GT); }
  "<=" { return symbol("leq",sym.LEQ); }
  ">=" { return symbol("geq",sym.GEQ); }
  "==" { return symbol("eq",sym.EQ); }
  "!=" { return symbol("neq",sym.NEQ); }
  "=" { return symbol("=",sym.ASSIGN); }
  "!" { return symbol("not",sym.NOT); }
  "||" { return symbol("or",sym.OR); }
  "&&" { return symbol("and",sym.AND); }

  /* comments */
  {Comment} { /* ignore */ }

  /* whitespace */
  {WhiteSpace} { /* ignore */ }

  /* literals */
  {Integer} { return symbol("IntLit", sym.INTEGER_LITERAL, new Integer(Integer.parseInt(yytext()))); }
  \" { yybegin(STRING); string.setLength(0); }

  {Identifier} { return symbol("Ident",sym.IDENTIFIER, yytext()); }
  {ClassName} {return symbol("Clas", sym.CNAME, yytext()); }
}

<STRING> {
  \" { yybegin(YYINITIAL); return symbol("StringLit", sym.STRING_LITERAL, string.toString()); }
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
<<EOF>> {return symbol("EOF",sym.EOF); }
