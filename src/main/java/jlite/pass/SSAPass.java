package jlite.pass;

import jlite.ir.Ir3;

/**
 * http://www.cs.cmu.edu/afs/cs/academic/class/15745-s12/public/lectures/L13-SSA-Concepts-1up.pdf
 * Use Dominance and Dominance Frontiers to Convert to SSA form
 */
public class SSAPass {

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            doMethod(method);
        }
    }

    private void doMethod(Ir3.Method method) {
        assert method.blocks != null; // assert basic block constructed
        assert method.dominance != null; // Check that dominance info is computed
    }
}
