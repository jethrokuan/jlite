package jlite.pass;

import jlite.ir.Ir3;

public class PassManager {
    public void run(Ir3.Prog ir3) {
        FlowPass flowPass = new FlowPass();
        flowPass.pass(ir3);
    }
}
