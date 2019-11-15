package jlite.pass;

import jlite.ir.Ir3;

import java.util.HashMap;

public class SpillPass {
    private final int TOTAL_REG_COUNT;
    private HashMap<Ir3.Var, Ir3.Block> defLocationMap;

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

    private void getNextUse(Ir3.Method method) {
    }


}
