package jlite.pass;

import com.google.common.collect.Lists;
import jlite.ir.Ir3;
import jlite.ir.LivenessInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Computes liveness information at the basic block level. Used for:
 * 1. Register allocation per block
 */
public class LivePass extends Pass {
    public HashMap<Ir3.Block, HashSet<Ir3.Var>> liveInMap = new HashMap<>();
    public HashMap<Ir3.Block, HashSet<Ir3.Var>> liveOutMap = new HashMap<>();
    public HashMap<Ir3.Stmt, HashSet<Ir3.Var>> stmtLiveInMap = new HashMap<>();
    public HashMap<Ir3.Stmt, HashSet<Ir3.Var>> stmtLiveOutMap = new HashMap<>();
    public HashMap<Ir3.Block, HashSet<Ir3.Var>> blockUseMap;
    public HashMap<Ir3.Block, HashSet<Ir3.Var>> blockDefMap;
    public HashMap<Ir3.Block, HashMap<Ir3.Var, Ir3.Stmt>> blockLastUse = new HashMap<>();
    Ir3.Method method;

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            pass(method);
        }
    }

    public void pass(Ir3.Method method) {
        this.method = method;
        initBlockUseDefs();
        initLives();
        dataflow();
        computeStmtLevel();
        method.liveness = new LivenessInfo(stmtLiveInMap, stmtLiveOutMap);
    }

    private void computeStmtLevel() {
        for (Ir3.Block block : method.blocks) {
            List<Ir3.Stmt> reverseOrder = Lists.reverse(block.statements);
            HashSet<Ir3.Var> currentLiveOut = liveOutMap.get(block);
            for (Ir3.Stmt stmt : reverseOrder) {
                stmtLiveOutMap.put(stmt, currentLiveOut);
                HashSet<Ir3.Var> liveIn = new HashSet<>();
                liveIn.addAll(currentLiveOut);
                liveIn.removeAll(stmt.getDefs());
                liveIn.addAll(stmt.getUses());
                stmtLiveInMap.put(stmt, liveIn);
                currentLiveOut = liveIn;
            }
            assert (currentLiveOut == liveInMap.get(block));
        }
    }

    private void initLives() {
        for (Ir3.Block block : method.blocks) {
            liveInMap.put(block, new HashSet<>());
            liveOutMap.put(block, new HashSet<>());
        }
    }

    private void dataflow() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Ir3.Block block : method.blockPostOrder) {
                HashSet<Ir3.Var> liveIn = liveInMap.get(block);
                HashSet<Ir3.Var> liveOut = liveOutMap.get(block);

                // OUT[B] = U IN[B]
                for (Ir3.Block outgoing : block.outgoing) {
                    liveOut.addAll(liveInMap.get(outgoing));
                }

                // IN[B] = f OUT[B]
                HashSet<Ir3.Var> newLiveIn = new HashSet<>();
                newLiveIn.addAll(liveOut);
                newLiveIn.removeAll(blockDefMap.get(block));
                newLiveIn.addAll(blockUseMap.get(block));
                if (!changed && !newLiveIn.equals(liveIn)) changed = true;
                liveInMap.put(block, newLiveIn);
            }
        }
    }

    private void initBlockUseDefs() {
        blockUseMap = new HashMap<>();
        blockDefMap = new HashMap<>();
        for (Ir3.Block block : method.blocks) {
            HashSet<Ir3.Var> blockUse = new HashSet<>();
            HashSet<Ir3.Var> blockDef = new HashSet<>();
            HashMap<Ir3.Var, Ir3.Stmt> blockLastUse = new HashMap<>();

            List<Ir3.Stmt> reverseOrder = Lists.reverse(block.statements);
            for (Ir3.Stmt stmt : reverseOrder) {
                for (Ir3.Var def : stmt.getDefs()) {
                    blockUse.remove(def);
                }

                for (Ir3.Var use : stmt.getUses()) {
                    if (!blockLastUse.containsKey(use)) blockLastUse.put(use, stmt);
                }
                blockUse.addAll(stmt.getUses());
                blockDef.addAll(stmt.getDefs());
            }
            blockUseMap.put(block, blockUse);
            blockDefMap.put(block, blockDef);
        }
    }
}