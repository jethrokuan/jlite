package jlite.pass;

import jlite.ir.Ir3;

import java.util.HashSet;
import java.util.Stack;

/**
 * Allocates registers to each var using graph colouring. Depends on liveness information.
 */
public class RegAllocPass extends Pass {
    Ir3.Method method;
    RegisterInterferenceGraph rig;
    Integer TOTAL_REG_COUNT = 12;
    Integer ARG_REGISTER_COUNT = 4;

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            pass(method);
        }
        PassUtils.write("_pass.regalloc", prog);
    }

    private void pass(Ir3.Method method) {
        this.method = method;
        LivePass livePass = new LivePass();
        livePass.pass(method);
        rig = new RegisterInterferenceGraph(method);
        preColor(method);
        Stack<Ir3.Var> processingOrder = rig.getProcessingOrder(TOTAL_REG_COUNT);
        for (int i = 0; i < ARG_REGISTER_COUNT && i < method.args.size(); i++) {
            Ir3.Var arg = method.args.get(i);
            arg.reg = i;
        }
        while (!processingOrder.empty()) {
            Ir3.Var toColor = processingOrder.pop();
            if (toColor.reg < 0) color(toColor);
        }
    }

    private void preColor(Ir3.Method method) {
        for (Ir3.Block block : method.blocks) {
            for (Ir3.Stmt stmt : block.statements) {
                if (!(stmt instanceof Ir3.CallStmt)) continue;
                Ir3.CallStmt callStmt = (Ir3.CallStmt) stmt;
                for (int i = 0; i < callStmt.args.size() && i < 4; i++) {
                    Ir3.Rval arg = callStmt.args.get(i);
                    assert arg instanceof Ir3.VarRval;
                    Ir3.VarRval varRval = (Ir3.VarRval) arg;
                    varRval.var.reg = i;
                }
                callStmt.lhs.reg = 0;
            }
        }
    }

    private void color(Ir3.Var toColor) {
        boolean[] availableColors = new boolean[TOTAL_REG_COUNT];
        HashSet<Ir3.Var> neighbours = rig.getNeighbours(toColor);
        for (Ir3.Var neighbour : neighbours) {
            if (neighbour.reg >= 0) availableColors[neighbour.reg] = true;
        }
        int availableColor = -1;
        for (int i = 0; i < TOTAL_REG_COUNT; i++) {
            if (!availableColors[i]) {
                availableColor = i;
                break;
            }
        }
        if (availableColor != -1) {
            toColor.reg = availableColor;
        } else {
            toColor.spilled = true;
            spill(toColor);
            pass(method);
        }
    }

    private void spill(Ir3.Var toSpill) {
        for (Ir3.Block block : method.blocks) {
            for (int i = 0; i < block.statements.size(); i++) {
                Ir3.Stmt stmt = block.statements.get(i);

                if (stmt.getUses().contains(toSpill)) {
                    Ir3.LoadStmt loadStmt = new Ir3.LoadStmt(toSpill);
                    block.statements.add(i, loadStmt);
                    i++;
                }
                if (stmt.getDefs().contains(toSpill)) {
                    block.statements.add(i + 1, new Ir3.StoreStmt(toSpill));
                    i++;
                }
            }
        }
    }
}
