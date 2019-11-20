package jlite.runners;

import jlite.StaticChecker;
import jlite.ir.Ir3;
import jlite.parser.Ast;
import jlite.parser.parser;
import jlite.pass.DominancePass;
import jlite.pass.FlowPass;
import jlite.pass.SSAPass;

import java.util.Arrays;

public class SSA {
    public static void main(String[] argv) {
        Arrays.stream(argv).forEach(fileLoc -> {
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                jlite.ir.Ir3Gen ir3Gen = new jlite.ir.Ir3Gen();
                Ir3.Prog ir3 = ir3Gen.gen(prog);
                FlowPass flowPass = new FlowPass();
                flowPass.pass(ir3); // Basic Block and CFG Construction
                DominancePass dominancePass = new DominancePass();
                dominancePass.pass(ir3); // Compute Dominance and Dominance Frontiers
                SSAPass ssaPass = new SSAPass();
                ssaPass.pass(ir3);
                System.out.println(ir3.print());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
