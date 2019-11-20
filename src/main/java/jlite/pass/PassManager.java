package jlite.pass;

import jlite.ir.Ir3;
import jlite.pass.optimizations.DeadCodeElimPass;

public class PassManager {

    public void run(Ir3.Prog ir3, boolean optimize) {
        FlowPass flowPass = new FlowPass();
        flowPass.pass(ir3); // Basic Block and CFG Construction
        LowerPass lowerPass = new LowerPass();
        lowerPass.pass(ir3);

        if (optimize) {
            DeadCodeElimPass deadCodeElimPass = new DeadCodeElimPass();
            deadCodeElimPass.pass(ir3);
        }
        LivePass livePass = new LivePass();
        livePass.pass(ir3);
        RegAllocPass regAllocPass = new RegAllocPass();
        regAllocPass.pass(ir3);
    }
}
