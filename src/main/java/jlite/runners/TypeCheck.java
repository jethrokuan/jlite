package jlite.runners;

import jlite.StaticChecker;
import jlite.parser.Ast;
import jlite.parser.parser;

public class TypeCheck {
    public static void main(String[] argv) {
        for (String fileLoc : argv) {
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                System.out.println("No type errors");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
