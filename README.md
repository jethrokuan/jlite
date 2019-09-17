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

```text
> Task :cupCompile UP-TO-DATE
> Task :jflex UP-TO-DATE
> Task :compileJava UP-TO-DATE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :ast
{
  "clasList": [
    {
      "cname": "Main",
      "varDeclList": [],
      "mdDeclList": [
        {
          "retTyp": {
            "typ": "VOID"
          },
          "name": "main",
          "args": [
            {
              "type": {
                "typ": "INTEGER"
              },
              "ident": "i"
            },
            {
              "type": {
                "typ": "INTEGER"
              },
              "ident": "a"
            },
            {
              "type": {
                "typ": "INTEGER"
              },
              "ident": "b"
            },
            {
              "type": {
                "typ": "INTEGER"
              },
              "ident": "d"
            }
          ],
          "vars": [],
          "stmts": [
            {
              "cond": {
                "val": true,
                "typ": "EXPR_BOOLLIT"
              },
              "stmtList": [
                {
                  "lhs": "b",
                  "rhs": {
                    "val": 340,
                    "typ": "EXPR_INTLIT"
                  },
                  "typ": "STMT_VARASSIGN"
                },
                {
                  "lhs": "t1",
                  "rhs": {
                    "ident": "t2",
                    "typ": "EXPR_IDENT"
                  },
                  "typ": "STMT_VARASSIGN"
                }
              ],
              "typ": "STMT_WHILE"
            }
          ]
        }
      ]
    },
    {
      "cname": "Dummy",
      "varDeclList": [
        {
          "type": {
            "cname": "Dummy",
            "typ": "CLASS"
          },
          "ident": "j"
        }
      ],
      "mdDeclList": [
        {
          "retTyp": {
            "typ": "INTEGER"
          },
          "name": "dummy",
          "args": [],
          "vars": [
            {
              "type": {
                "typ": "BOOLEAN"
              },
              "ident": "i"
            },
            {
              "type": {
                "typ": "BOOLEAN"
              },
              "ident": "j"
            }
          ],
          "stmts": [
            {
              "expr": {
                "ident": "i",
                "typ": "EXPR_IDENT"
              },
              "typ": "STMT_RETURN"
            }
          ]
        }
      ]
    }
  ]
}

BUILD SUCCESSFUL in 0s
4 actionable tasks: 1 executed, 3 up-to-date
```

To print the code:

```
./gradlew run --args="test/pass/e.j"
```

```
> Task :cupCompile UP-TO-DATE
> Task :jflex UP-TO-DATE
> Task :compileJava UP-TO-DATE
> Task :processResources NO-SOURCE
> Task :classes UP-TO-DATE

> Task :run
class Main {

  Void main(Int i, Int a, Int b, Int d) {
    while (true) {
      b = 340;
      t1 = t2;
    }
  }
}

class Dummy {
  Dummy j;

  Int dummy() {
    Bool i;
    Bool j;
    return i;
  }
}



BUILD SUCCESSFUL in 0s
4 actionable tasks: 1 executed, 3 up-to-date
```

## Failed Cases

To see an example of a failed parse, and parse error, try:

```
./gradlew run --args="test/fail/missing_else.j"
```

``` text
test/fail/missing_lparen.j:2:9 parse error:
class Main {
Void main(Int i, Int a, Int b,Int d) {
   while true {
~~~~~~~~~^
      b = 340 ;
      t1 = t2 ;


instead expected token classes are [LPAREN]
```
