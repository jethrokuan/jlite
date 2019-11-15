package jlite.pass;

import jlite.ir.Ir3;

public class PassManager {
    public void run(Ir3.Prog ir3) {
        FlowPass flowPass = new FlowPass();
        flowPass.pass(ir3); // Basic Block and CFG Construction
        DominancePass dominancePass = new DominancePass();
        dominancePass.pass(ir3); // Compute Dominance and Dominance Frontiers
        SSAPass ssaPass = new SSAPass();
        ssaPass.pass(ir3);
        WebPass webPass = new WebPass();
        webPass.pass(ir3);
        SpillPass spillPass = new SpillPass(13);
        spillPass.pass(ir3);
    }
}
