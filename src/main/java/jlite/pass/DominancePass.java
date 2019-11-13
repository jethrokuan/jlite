package jlite.pass;

import com.google.common.collect.Lists;
import jlite.ir.DominanceInfo;
import jlite.ir.Ir3;

import java.util.HashMap;
import java.util.List;

public class DominancePass {
    HashMap<Ir3.Block, Ir3.Block> idom = new HashMap<>();

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            method.dominance = computeDominance(method);
        }
    }

    private DominanceInfo computeDominance(Ir3.Method method) {
        if (method.blocks.isEmpty()) {
            return new DominanceInfo(idom);
        }

        Ir3.Block firstBlock = method.blocks.get(0);
        idom.put(firstBlock, null);
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Ir3.Block> reversePostOrder = Lists.reverse(method.blockPostOrder);
            for (Ir3.Block block : reversePostOrder) {
                if (block == firstBlock) continue;
                Ir3.Block newIdom = null;
                for (Ir3.Block other : block.incoming) {
                    if (!idom.containsKey(other)) continue;
                    if (newIdom == null) newIdom = other;
                    else newIdom = intersect(other, newIdom);
                }

                if (!idom.containsKey(block) || idom.get(block) != newIdom) {
                    idom.put(block, newIdom);
                    changed = true;
                }
            }
        }
        return new DominanceInfo(idom);
    }

    private Ir3.Block intersect(Ir3.Block b1, Ir3.Block b2) {
        Ir3.Block f1 = b1;
        Ir3.Block f2 = b2;
        while (f1 != f2) {
            if (f2 == null) {
                f1 = idom.get(f1);
            } else if (f1 == null) {
                f2 = idom.get(f2);
            } else if (f1.postOrderIndex < f2.postOrderIndex) {
                f1 = idom.get(f1);
            } else if (f2.postOrderIndex < f1.postOrderIndex) {
                f2 = idom.get(f2);
            }
        }
        return f1;
    }
}
