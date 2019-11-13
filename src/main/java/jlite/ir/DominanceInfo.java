package jlite.ir;

import java.util.HashMap;

public class DominanceInfo {
    HashMap<Ir3.Block, Ir3.Block> idom;

    public DominanceInfo(HashMap<Ir3.Block, Ir3.Block> idom) {
        this.idom = idom;
    }
}
