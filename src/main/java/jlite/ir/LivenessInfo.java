package jlite.ir;

import java.util.HashMap;
import java.util.HashSet;

public class LivenessInfo {
    public HashMap<Ir3.Stmt, HashSet<Ir3.Var>> stmtLiveInMap;
    public HashMap<Ir3.Stmt, HashSet<Ir3.Var>> stmtLiveOutMap;

    public LivenessInfo(HashMap<Ir3.Stmt, HashSet<Ir3.Var>> stmtLiveInMap, HashMap<Ir3.Stmt, HashSet<Ir3.Var>> stmtLiveOutMap) {
        this.stmtLiveInMap = stmtLiveInMap;
        this.stmtLiveOutMap = stmtLiveOutMap;
    }
}
