package jlite.pass;

import jlite.ir.Ir3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * http://www.cs.cmu.edu/afs/cs/academic/class/15745-s12/public/lectures/L13-SSA-Concepts-1up.pdf
 * Use Dominance and Dominance Frontiers to Convert to SSA form
 */
public class SSAPass {

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            doMethod(method);
        }
        System.out.print(prog.print());
    }

    /**
     * 1. Place all Phi
     * 2. Rename all variables
     *
     * @param method Ir3 Method to process
     */
    private void doMethod(Ir3.Method method) {
        assert method.blocks != null; // assert basic block constructed
        assert method.dominance != null; // Check that dominance info is computed
        placePhis(method);
    }

    private void placePhis(Ir3.Method method) {
        HashMap<Ir3.Block, HashSet<Ir3.Var>> orig = new HashMap<>();
        HashMap<Ir3.Var, HashSet<Ir3.Block>> defSites = new HashMap<>();

        for (Ir3.Block block : method.blocks) {
            for (Ir3.Stmt stmt : block.statements) {
                for (Ir3.Var def : stmt.getDefs()) {
                    HashSet<Ir3.Var> o = orig.getOrDefault(block, new HashSet<>());
                    o.add(def);
                    orig.put(block, o);
                    HashSet<Ir3.Block> ds = defSites.getOrDefault(def, new HashSet<>());
                    ds.add(block);
                    defSites.put(def, ds);
                }
            }
        }

        ArrayList<Ir3.Var> vars = new ArrayList<>();
        vars.addAll(method.args);
        vars.addAll(method.locals);

        HashSet<Ir3.Block> W;
        HashSet<Ir3.Block> P;
        for (Ir3.Var v : vars) {
            W = new HashSet<>();
            P = new HashSet<>();
            W.addAll(defSites.getOrDefault(v, new HashSet<>()));

            while (!W.isEmpty()) {
                Ir3.Block n = W.iterator().next();
                W.remove(n);
                HashSet<Ir3.Block> frontier = method.dominance.frontier.get(n);
                for (Ir3.Block y : frontier) {
                    if (!P.contains(y)) {
                        y.statements.add(0, new Ir3.PhiStmt(v, y.incoming.size()));
                        P.add(y);
                        HashSet<Ir3.Var> o = orig.getOrDefault(y, new HashSet<>());
                        if (!o.contains(v)) W.add(y);
                    }
                }
            }
        }

    }
}
