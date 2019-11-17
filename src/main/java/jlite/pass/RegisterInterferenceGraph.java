package jlite.pass;

import jlite.ir.Ir3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

public class RegisterInterferenceGraph {
    HashMap<Ir3.Var, HashSet<Ir3.Var>> adjList = new HashMap<>();

    public RegisterInterferenceGraph(Ir3.Method method) {
        for (Ir3.Var arg : method.args) {
            adjList.put(arg, new HashSet<>());
        }

        for (Ir3.Var local : method.locals) {
            adjList.put(local, new HashSet<>());
        }

        for (Ir3.Block block : method.blocks) {
            for (Ir3.Stmt stmt : block.statements) {
                HashSet<Ir3.Var> liveOutInfo = method.liveness.stmtLiveOutMap.get(stmt);
                HashSet<Ir3.Var> workingSet = new HashSet<>(liveOutInfo);
                while (!workingSet.isEmpty()) {
                    Ir3.Var a = workingSet.iterator().next();
                    workingSet.remove(a);
                    if (!adjList.containsKey(a)) {
                        adjList.put(a, new HashSet<>());
                    }
                    for (Ir3.Var b : workingSet) {
                        // Add the relation between a and b
                        HashSet<Ir3.Var> aNeighbours = adjList.get(a);
                        HashSet<Ir3.Var> bNeighbours = adjList.getOrDefault(b, new HashSet<>());
                        aNeighbours.add(b);
                        bNeighbours.add(a);
                        adjList.put(a, aNeighbours);
                        adjList.put(b, bNeighbours);
                    }
                }
            }
        }
    }

    public Ir3.Var findNode(HashMap<Ir3.Var, HashSet<Ir3.Var>> workingAdjList, Integer count) {
        for (Map.Entry<Ir3.Var, HashSet<Ir3.Var>> entry : workingAdjList.entrySet()) {
            if (entry.getValue().size() < count) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Stack<Ir3.Var> getProcessingOrder(Integer size) {
        Stack<Ir3.Var> stack = new Stack<>();
        HashMap<Ir3.Var, HashSet<Ir3.Var>> workingAdjList = new HashMap<>();
        for (Map.Entry<Ir3.Var, HashSet<Ir3.Var>> entry : adjList.entrySet()) {
            workingAdjList.put(entry.getKey(), entry.getValue());
        }

        while (!workingAdjList.isEmpty()) {
            Ir3.Var chosen = findNode(workingAdjList, size);
            if (chosen == null) { // Choose any node
                chosen = getHighestDegree(workingAdjList);
            }
            HashSet<Ir3.Var> neighbours = workingAdjList.get(chosen);
            for (Ir3.Var neighbour : neighbours) {
                workingAdjList.get(neighbour).remove(chosen);
            }
            workingAdjList.remove(chosen);
            stack.push(chosen);
        }
        return stack;
    }

    private Ir3.Var getHighestDegree(HashMap<Ir3.Var, HashSet<Ir3.Var>> workingAdjList) {
        Integer highestDegree = -1;
        Ir3.Var var = null;
        for (Map.Entry<Ir3.Var, HashSet<Ir3.Var>> entry : workingAdjList.entrySet()) {
            Integer degree = entry.getValue().size();
            if (!entry.getKey().spilled && degree > highestDegree) {
                var = entry.getKey();
                highestDegree = degree;
            }
        }
        return var;
    }

    public HashSet<Ir3.Var> getNeighbours(Ir3.Var toColor) {
        return adjList.get(toColor);
    }
}
