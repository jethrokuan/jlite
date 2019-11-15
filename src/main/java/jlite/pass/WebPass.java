package jlite.pass;

import jlite.ir.Ir3;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Construction of Webs for use in SSA-based register allocation
 * Uses UFDS as described in class
 */
public class WebPass {
    private ArrayList<Ir3.Web> webs;
    private HashMap<Ir3.Var, Ir3.Web> varToWebMap;
    private UFDS ufds;

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            doMethod(method);
        }
    }

    private void doMethod(Ir3.Method method) {
        webs = new ArrayList<>();
        varToWebMap = new HashMap<>();
        ufds = new UFDS(method);

        for (Ir3.Var arg : method.args) {
            doVar(arg, method);
        }

        for (Ir3.Var local : method.locals) {
            doVar(local, method);
        }

        method.webs = webs;
    }

    private void doVar(Ir3.Var v, Ir3.Method method) {
        Ir3.Var parent = ufds.find(v);
        Ir3.Web web;
        if (varToWebMap.containsKey(parent)) {
            web = varToWebMap.get(parent);
        } else {
            web = new Ir3.Web();
            varToWebMap.put(parent, web);
            webs.add(web);
        }
        v.web = web;
    }

    private class UFDS {
        public HashMap<Ir3.Var, Ir3.Var> P;
        public HashMap<Ir3.Var, Integer> Rank;

        public UFDS(Ir3.Method method) {
            P = new HashMap<>();
            Rank = new HashMap<>();

            for (Ir3.Var arg : method.args) {
                P.put(arg, arg);
                Rank.put(arg, 0);
            }

            for (Ir3.Var local : method.locals) {
                P.put(local, local);
                Rank.put(local, 0);
            }

            for (Ir3.Block block : method.blocks) {
                for (Ir3.Stmt stmt : block.statements) {
                    if (stmt instanceof Ir3.PhiStmt) {
                        Ir3.PhiStmt phiStmt = (Ir3.PhiStmt) stmt;
                        for (Ir3.Var v : phiStmt.args) {
                            union(phiStmt.var, v);
                        }
                    }
                }
            }
        }

        private void union(Ir3.Var v1, Ir3.Var v2) {
            Ir3.Var p1 = find(v1);
            Ir3.Var p2 = find(v2);
            if (p1 != p2) link(p1, p2);
        }

        private void link(Ir3.Var v1, Ir3.Var v2) {
            if (Rank.get(v1) > Rank.get(v2)) {
                P.put(v2, v1);
            } else {
                P.put(v1, v2);
            }

            if (Rank.get(v1) == Rank.get(v2)) {
                Rank.put(v2, Rank.get(v2) + 1);
            }
        }

        private Ir3.Var find(Ir3.Var v) {
            if (P.get(v) != v) P.put(v, find(P.get(v)));
            return P.get(v);
        }
    }
}
