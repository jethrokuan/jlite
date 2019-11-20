package jlite.pass;

import jlite.ir.Ir3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 1. Compute liveness and global next uses (Section 4.1):
 * We present a modification to the standard data-flow formulation of liveness analysis
 * to compute CFG-global next-use values for each variable.
 * 2. For each block B in reverse post order of the CFG:
 * (a) Determine initialization W B entry of register set (Section 4.2):
 * We compute a set of variables, which we assume to be in registers at the entry
 * of the block.
 * (b) Insert coupling code at the block entry (Section 4.3):
 * Depending on the state of the register file at the exit of the predecessor of B,
 * we add spill and/or reload code on B’s incoming control-flow edges to ensure
 * that all variables in W B entry are indeed in registers at the entry of B
 * (c) Perform MIN Algorithm on B
 * 3. Reconstruct SSA
 */
public class SpillPass extends Pass {
    private final int TOTAL_REG_COUNT;
    private HashMap<Ir3.Var, Ir3.Block> defLocationMap;
    private HashMap<Ir3.Block, SpillInfo> blockSpillInfoHashMap;
    private HashMap<Ir3.Block, NextUseInfo> blockNextUseInfoHashMap;

    public SpillPass(int register_count) {
        TOTAL_REG_COUNT = register_count;
    }

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            doMethod(method);
        }
    }

    private void doMethod(Ir3.Method method) {
        getDefs(method);
        getNextUse(method);
        initW(method);
    }

    private void initW(Ir3.Method method) {
        blockSpillInfoHashMap = new HashMap<>();
        for (Ir3.Block block : method.blocks) {
            blockSpillInfoHashMap.put(block, new SpillInfo());
        }
        Ir3.Block first = method.blocks.get(0);
        for (Ir3.Block block : method.blocks) {
            SpillInfo spillInfo = blockSpillInfoHashMap.get(block);
            HashSet<Ir3.Var> W = new HashSet<>();
            HashSet<Ir3.Var> S = new HashSet<>();
            if (block == first) {
                for (int i = 0; i < 4 && i < method.args.size(); i++) {
                    W.add(method.args.get(i));
                }
                minAlgo(block, W, S);
                spillInfo.Wexit = W;
                spillInfo.Sexit = new HashSet<>();
            }
        }
    }

    /**
     * @param block
     * @param W     variables in reg, |W| < total_reg_count
     * @param S     variables spilled
     */
    private void minAlgo(Ir3.Block block, HashSet<Ir3.Var> W, HashSet<Ir3.Var> S) {
    }

    private void getDefs(Ir3.Method method) {
        defLocationMap = new HashMap<>();
        Ir3.Block first = method.blocks.get(0);
        for (Ir3.Var arg : method.args) {
            defLocationMap.put(arg, first);
        }

        for (Ir3.Block block : method.blocks) {
            for (Ir3.Stmt stmt : block.statements) {
                for (Ir3.Var def : stmt.getDefs()) {
                    defLocationMap.put(def, block);
                }
            }
        }
    }

    /**
     * Dataflow Analysis with modifications:
     *   1. domain = Var -> Natural Numbers + infty
     *   2. meet operator = min(a(v), b(v)) for all v
     *   3. Transfer function = \lambda v . length_B + { v_B(v) if v_B(v) != infty ; |B| + a(v) otherwise }
     */
    private void getNextUse(Ir3.Method method) {
        reinitNextUseInfo(method);
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Ir3.Block block : method.blockPostOrder) {
                NextUseInfo nextUseInfo = blockNextUseInfoHashMap.get(block);

                // Recompute OUT[B]
                for (Ir3.Block out : block.outgoing) {
                    NextUseInfo outNextUseInfo = blockNextUseInfoHashMap.get(out);
                    for (Map.Entry<Ir3.Var, Integer> entry : outNextUseInfo.in.entrySet()) {
                        Ir3.Var key = entry.getKey();
                        Integer value = entry.getValue();

                        if (!nextUseInfo.out.containsKey(key) || value < nextUseInfo.out.get(key))
                            nextUseInfo.out.put(key, value);
                    }
                }

                HashSet<Ir3.Var> seen = new HashSet<>();
                for (int idx = 0; idx < block.statements.size(); idx++) {
                    Ir3.Stmt stmt = block.statements.get(idx);
                    if (stmt instanceof Ir3.JumpStmt) {
                        Set<Ir3.Var> uses = getJumpUses(block);
                        for (Ir3.Var use : uses) {
                            if (seen.contains(use)) continue;
                            if (!nextUseInfo.in.containsKey(use) || idx < nextUseInfo.in.get(use)) {
                                changed = true;
                                nextUseInfo.in.put(use, idx);
                            }
                            seen.add(use);
                        }

                        for (Ir3.Var def : stmt.getDefs()) {
                            if (def != null) seen.add(def);
                        }
                    }
                }

                for (Map.Entry<Ir3.Var, Integer> entry : nextUseInfo.out.entrySet()) {
                    Ir3.Var key = entry.getKey();
                    if (seen.contains(key)) continue;
                    int V = entry.getValue() + block.statements.size();
                    if (!nextUseInfo.in.containsKey(key) || V < nextUseInfo.in.get(key)) {
                        changed = true;
                        nextUseInfo.in.put(key, V);
                    }
                }
            }
        }
    }

    private Set<Ir3.Var> getJumpUses(Ir3.Block block) {
        HashSet<Ir3.Var> uses = new HashSet<>();
        for (Ir3.Block out : block.outgoing) {
            int incomingIdx = out.incoming.indexOf(block);
            for (Ir3.Stmt stmt : out.statements) {
                if (!(stmt instanceof Ir3.PhiStmt)) break;
                Ir3.PhiStmt phiStmt = (Ir3.PhiStmt) stmt;
                if (!phiStmt.memory && phiStmt.args.get(incomingIdx) != null) {
                    uses.add(phiStmt.args.get(incomingIdx));
                }
            }
        }
        return uses;
    }

    private void reinitNextUseInfo(Ir3.Method method) {
        blockNextUseInfoHashMap = new HashMap<>();
        for (Ir3.Block block : method.blocks) {
            blockNextUseInfoHashMap.put(block, new NextUseInfo());
        }
    }


    /**
     * Map Var -> Natural Numbers + infty
     */
    private class NextUseInfo {
        HashMap<Ir3.Var, Integer> in = new HashMap<>();
        HashMap<Ir3.Var, Integer> out = new HashMap<>();
    }

    private class SpillInfo {
        HashSet<Ir3.Var> Wentry = new HashSet<>(); // Variables in Reg on entry
        HashSet<Ir3.Var> Wexit = new HashSet<>(); // Variables in Reg on exit
        HashSet<Ir3.Var> Sentry = new HashSet<>(); // Variables spilled on entry
        HashSet<Ir3.Var> Sexit = new HashSet<>(); // Variables spilled on exit
    }
}
