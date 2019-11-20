package jlite.runners;

import jlite.StaticChecker;
import jlite.ir.Ir3;
import jlite.parser.Ast;
import jlite.parser.parser;

import java.util.Arrays;

public class Ir3Gen {
    public static void main(String[] argv) {
        Arrays.stream(argv).forEach(fileLoc -> {
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                jlite.ir.Ir3Gen ir3Gen = new jlite.ir.Ir3Gen();
                Ir3.Prog ir3 = ir3Gen.gen(prog);
                System.out.println(ir3.print());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
