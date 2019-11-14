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
    private ArrayList<Ir3.Var> newLocals = new ArrayList<>();
    private HashMap<Ir3.Var, Ir3.Var> reachingDefMap;
    private HashMap<Ir3.Var, Ir3.Block> defMap;
    private int counter = 0;

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            doMethod(method);
        }
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
        renameVariables(method);
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

    private void renameVariables(Ir3.Method method) {
        Ir3.Block first = method.blocks.get(0);
        reachingDefMap = new HashMap<>();
        defMap = new HashMap<>();

        for (Ir3.Var v : method.args) {
            reachingDefMap.put(v, v);
            defMap.put(v, first);
        }

        for (Ir3.Var v : method.locals) {
            defMap.put(v, first);
        }

        for (Ir3.Block block : method.dominance.preorder) {
            for (Ir3.Stmt stmt : block.statements) {
                for (Ir3.Rval rval : stmt.getRvals()) {
                    if (!(rval instanceof Ir3.VarRval)) continue;
                    Ir3.VarRval varRval = (Ir3.VarRval) rval;
                    Ir3.Var newVar = doUse(varRval.var, block, method);
                    varRval.var = newVar;
                }

                if (stmt instanceof Ir3.FieldAssignStatement) {
                    Ir3.FieldAssignStatement fieldAssignStatement = (Ir3.FieldAssignStatement) stmt;
                    fieldAssignStatement.target = doUse(fieldAssignStatement.target, block, method);
                }

                for (Ir3.Var def : stmt.getDefs()) {
                    if (def == null) continue;
                    updateReachingDef(def, block, method);
                    Ir3.Var newVar = getNewVar(def);
                    newLocals.add(newVar);
                    stmt.updateDef(newVar);
                    defMap.put(newVar, block);
                    reachingDefMap.put(newVar, reachingDefMap.get(def));
                    reachingDefMap.put(def, newVar);
                }
            }

            for (Ir3.Block b : block.outgoing) {
                for (Ir3.Stmt s : b.statements) {
                    if (!(s instanceof Ir3.PhiStmt)) break;
                    Ir3.PhiStmt phiStmt = (Ir3.PhiStmt) s;
                    doPhi(phiStmt, block, b, method);
                }
            }

            method.locals = newLocals;
        }
    }

    private void updateReachingDef(Ir3.Var def, Ir3.Block block, Ir3.Method method) {
        Ir3.Var r = reachingDefMap.get(def);
        while (!(r == null || method.dominance.dominates(defMap.get(r), block)))
            r = reachingDefMap.get(r);
        reachingDefMap.put(def, r);
    }

    private Ir3.Var doUse(Ir3.Var use, Ir3.Block block, Ir3.Method method) {
        updateReachingDef(use, block, method);
        Ir3.Var reach = reachingDefMap.get(use);
        if (reach == null) {
            reach = getNewVar(use);
            newLocals.add(reach);
            defMap.put(reach, block);
            reachingDefMap.put(use, reach);
        }
        return reach;
    }

    private void doPhi(Ir3.PhiStmt phiStmt, Ir3.Block incomingB, Ir3.Block phiB, Ir3.Method method) {
        updateReachingDef(phiStmt.var, incomingB, method);
        Ir3.Var v = reachingDefMap.get(phiStmt.var);
        if (v != null) {
            int index = phiB.incoming.indexOf(incomingB);
            phiStmt.args.set(index, v);
        }
    }

    private Ir3.Var getNewVar(Ir3.Var oldvar) {
        return new Ir3.Var(oldvar.typ, String.format("%s#%s", oldvar.name, counter++));
    }
}
