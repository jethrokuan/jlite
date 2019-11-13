package jlite.ir;

import java.util.HashMap;
import java.util.HashSet;

public class DominanceInfo {
    public HashMap<Ir3.Block, HashSet<Ir3.Block>> frontier;
    public HashMap<Ir3.Block, Ir3.Block> idom;

    public DominanceInfo(HashMap<Ir3.Block, Ir3.Block> idom) {
        this.idom = idom;
        this.frontier = new HashMap<>();
    }
}
