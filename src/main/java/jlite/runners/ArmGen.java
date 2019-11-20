package jlite.runners;

import jlite.StaticChecker;
import jlite.arm.Arm;
import jlite.ir.Ir3;
import jlite.ir.Ir3Gen;
import jlite.parser.Ast;
import jlite.parser.parser;
import jlite.pass.ArmGenPass;
import jlite.pass.PassManager;

import java.util.Arrays;

public class ArmGen {
    public static void main(String[] args) {
        Arrays.stream(args).forEach(fileLoc -> {
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                jlite.ir.Ir3Gen ir3Gen = new Ir3Gen();
                Ir3.Prog ir3 = ir3Gen.gen(prog);
                PassManager passManager = new PassManager();
                passManager.run(ir3, false);
                ArmGenPass armGenPass = new ArmGenPass();
                Arm.Prog armProg = armGenPass.pass(ir3);
                System.out.print(armProg.print());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
