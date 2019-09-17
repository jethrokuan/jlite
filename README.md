# JLite parser

This is a compiler built for NUS CS4212.

## Run Instructions

This project uses:

1. JFlex to specify the lexer
2. JCup to specify the parser
3. GSON for printing purposes

We use Gradle as our build system.

To run only the lexer:

```
./gradlew lex --args="test/pass/e.j"
```

To create and print the AST:

```
./gradlew ast --args="test/pass/e.j"
```

To print the code:

```
./gradlew run --args="test/pass/e.j"
```

## Failed Cases

To see an example of a failed parse, and parse error, try:

```
./gradlew run --args="test/fail/fail.j"
```

``` text
test/fail/fail2.j:11:14 parse error:
class Dummy {
   Dummy j;
   Int dummy( {
~~~~~~~~~~~~~~^
      Bool i;
      Bool j;


instead expected token classes are [BOOL, STRING, CNAME, RPAREN]
```
