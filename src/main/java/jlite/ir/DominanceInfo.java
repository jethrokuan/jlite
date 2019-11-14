package jlite.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DominanceInfo {
    public HashMap<Ir3.Block, HashSet<Ir3.Block>> frontier;
    public HashMap<Ir3.Block, Ir3.Block> idom;
    public ArrayList<Ir3.Block> preorder;
    public ArrayList<Ir3.Block> postorder;

    public DominanceInfo(HashMap<Ir3.Block, Ir3.Block> idom) {
        this.idom = idom;
        this.frontier = new HashMap<>();
    }

    public boolean dominates(Ir3.Block b1, Ir3.Block b2) {
        assert b1 != null;
        assert b2 != null;
        while (b2 != null && b2 != b1) {
            b2 = idom.get(b2);
        }
        return b2 == b1;
    }
}
