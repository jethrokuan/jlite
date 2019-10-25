# Project Assignment 2

## Typechecking (COMPLETE)

Typechecking is performed in the StaticCheck.java file.

Method overloading is handled by using a Multi-map, storing both the method name and the method signature.
The tuple <name, signature> was not allowed to be repeated by the Typechecker.

The Environment has 2 functions: get() and getOne().

get() allows the return of multiple candidates: this happens when there are methods of the same name with different signatures.

getOne() enforces the return of only one type: this should be the case where the type is any other primitive (e.g. Int).

E.g.
```bash
./gradlew tc --args="test/staticchecker/method_sig_clash.j"

> Task :tc
jlite.exceptions.SemanticException: 11:3: Duplicate method signature '[Int]->Void' for method 'm1' of class 'Dummy'
        at jlite.StaticChecker.init(StaticChecker.java:477)
        at jlite.StaticChecker.run(StaticChecker.java:13)
        at jlite.StaticChecker.main(StaticChecker.java:499)
```

## Codegen (INCOMPLETE)

IR3 codegeneration is done in the Ir3Gen.java file, utilizing classes in the Ir3.java file. Codegen is done after a typechecking pass,
which imbues the AST with type information for all expressions.

Labels are generated globally (unique across the whole program), while temporaries are scoped to the method. These are handled by
tempGenerator and labelGenerator.

Control-flow expressions are handled via backpatching as described in the Dragon book.

Some expression types were not handled because I was unable to successfully figure out how to do so. These include call and ident expressions. Hence,
the IR3 code generation portion is incomplete.

### Done:
- Name-mangling
- Control-flow expressions via backpatching
- generation of labels and temporaries
- handling of arithmetic and logical expressions (e.g. j = 3 + 5 + 7)

### Incomplete:
- Expressions requiring resolution (e.g. call, dot expressions)

```text
./gradlew ir3 --args="test/ir/test.j" 

> Task :ir3
======= CData3 =======
Data3 MainC{
}

Data3 Functional{
  Int a;
}

======= CMtd3 =======
Void main(this){
  Int _t0;
  Int _t1;
  Functional _t2;
  String _t3;
  Functional fo;
  Int i;
  Int j;
  readln(i);
  Label0:
  _t0 = 0;
  if (j > _t0) goto Label1;
  goto Label4;
  Label1:
  println(j);
  goto Label0;
  Label4:
  _t1 = 0;
  if (i > _t1) goto Label2;
  goto Label3;
  Label2:
  _t2 = new Functional();
  fo = _t2;
  println(j);
  goto Label5;
  Label3:
  _t3 = "Error";
  println(_t3);
  Label5:
  return ;
}

Int %Functional_f_0(this, b){
  Int _t4;
  _t4 = 3;
  return _t4;
}

=====fx== End of IR3 Program =======

```