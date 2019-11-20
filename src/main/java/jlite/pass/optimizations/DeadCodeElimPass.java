package jlite.pass.optimizations;

import jlite.ir.Ir3;
import jlite.pass.LivePass;
import jlite.pass.PassUtils;

import java.util.ArrayList;

/**
 * Dead Code Elimination, depends on liveness analysis
 * If a statement defines something that is not live on output, it is safe to delete.
 */
public class DeadCodeElimPass {
    public void pass(Ir3.Prog prog) {
        LivePass livePass = new LivePass();
        livePass.pass(prog);
        while (elim(prog)) {
            livePass.pass(prog);
        }
        PassUtils.write("_pass.deadcodeelim", prog);
    }

    private boolean elim(Ir3.Prog ir3) {
        boolean hasChange = false;
        for (Ir3.Method method : ir3.methods) {
            for (Ir3.Block block : method.blocks) {
                ArrayList<Ir3.Stmt> newStmts = new ArrayList<>();
                for (Ir3.Stmt stmt : block.statements) {
                    if (!stmt.getDefs().isEmpty()) {
                        boolean defsAllDead = true;
                        for (Ir3.Var def : stmt.getDefs()) {
                            if (method.liveness.stmtLiveOutMap.get(stmt).contains(def)) {
                                defsAllDead = false;
                                break;
                            }
                        }
                        if (!defsAllDead) newStmts.add(stmt);
                        else hasChange = true;
                    } else {
                        newStmts.add(stmt);
                    }
                }
                block.statements = newStmts;
            }
        }
        return hasChange;
    }
}
