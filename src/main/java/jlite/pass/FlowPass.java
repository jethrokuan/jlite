package jlite.pass;

import jlite.ir.Ir3;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

/**
 * The first three-address instruction of the intermediate code is a leader.
 * Instructions which are targets of jump or conditional jump are leaders.
 * Instructions which immediately follows jump are considered as leaders
 */
public class FlowPass {
    private int postOrderIndex = 0;
    private int preOrderIndex = 0;

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            doMethod(method);
        }
    }

    public void doMethod(Ir3.Method method) {
        ArrayList<Ir3.Block> blocks = new ArrayList<>();
        BitSet leaders = new BitSet(method.statements.size());
        ArrayList<Ir3.LabelStmt> usedLabels = new ArrayList<>();
        HashMap<Ir3.LabelStmt, Ir3.Block> labelToBlock = new HashMap<>();
        leaders.clear();

        boolean prevIsJump = false;

        for (int i = 0; i < method.statements.size(); i++) {
            Ir3.Stmt stmt = method.statements.get(i);
            if (prevIsJump) {
                leaders.set(i, true);
            }

            if (stmt instanceof Ir3.JumpStmt) {
                Ir3.JumpStmt jumpStmt = (Ir3.JumpStmt) stmt;
                usedLabels.add(jumpStmt.label);
                prevIsJump = true;
            } else {
                prevIsJump = false;
            }
        }

        for (int i = 0; i < method.statements.size(); i++) {

            Ir3.Stmt stmt = method.statements.get(i);
            if (stmt instanceof Ir3.LabelStmt) {
                Ir3.LabelStmt labelStmt = (Ir3.LabelStmt) stmt;
                if (usedLabels.contains(labelStmt)) {
                    leaders.set(i, true);
                }
            }
        }

        leaders.set(0, true); // First statement is a leader

        // Start new block
        Ir3.Block block = new Ir3.Block();
        block.statements.add(method.statements.get(0));
        for (int i = 1; i < method.statements.size(); i++) {
            if (leaders.get(i)) { // new leader
                blocks.add(block);
                block = new Ir3.Block();
            }
            block.statements.add(method.statements.get(i));
        }
        if (!block.statements.isEmpty()) {
            blocks.add(block);
        }

        // Generate labels
        int count = 0;
        HashMap<Ir3.LabelStmt, Ir3.LabelStmt> blockLabels = new HashMap<>();
        for (Ir3.Block b : blocks) {
            b.labelStmt = new Ir3.LabelStmt(String.format("B%s", count));
            if (b.statements.get(0) instanceof Ir3.LabelStmt) {
                blockLabels.put((Ir3.LabelStmt) b.statements.get(0), b.labelStmt);
                b.statements.remove(0);
            }
            labelToBlock.put(b.labelStmt, b);
            count++;
        }

        for (Ir3.Block b : blocks) {
            for (Ir3.Stmt stmt : b.statements) {
                if (stmt instanceof Ir3.JumpStmt) {
                    Ir3.JumpStmt jumpStmt = (Ir3.JumpStmt) stmt;
                    Ir3.LabelStmt labelStmt = blockLabels.get(jumpStmt.getLabel());
                    assert labelStmt != null;
                    jumpStmt.setLabel(labelStmt);
                }
            }
        }

        // Add edges
        for (int i = 0; i < blocks.size() - 1; i++) {
            Ir3.Block fromBlock = blocks.get(i);

            if (fromBlock.statements.isEmpty()) {
                Ir3.Block toBlock = blocks.get(i + 1);
                Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt(toBlock.labelStmt);
                fromBlock.statements.add(gotoStmt);
                fromBlock.outgoing.add(toBlock);
                continue;
            }

            Ir3.Stmt lastStmt = fromBlock.statements.get(fromBlock.statements.size() - 1);

            if (lastStmt instanceof Ir3.ReturnStmt) continue;
            if (!(lastStmt instanceof Ir3.JumpStmt)) {
                Ir3.Block toBlock = blocks.get(i + 1);
                fromBlock.outgoing.add(toBlock);
                continue;
            }

            if (lastStmt instanceof Ir3.GotoStmt) {
                Ir3.GotoStmt gotoStmt = (Ir3.GotoStmt) lastStmt;
                Ir3.Block toBlock = labelToBlock.get(gotoStmt.label);
                fromBlock.outgoing.add(toBlock);
            } else if (lastStmt instanceof Ir3.CmpStmt) {
                Ir3.CmpStmt cmpStmt = (Ir3.CmpStmt) lastStmt;
                Ir3.Block trueBlock = labelToBlock.get(cmpStmt.label);
                Ir3.Block fallThruBlock = blocks.get(i + 1);
                fromBlock.outgoing.add(trueBlock);
                fromBlock.outgoing.add(fallThruBlock);
            } else {
                continue;
            }
        }

        for (Ir3.Block b : blocks) {
            for (Ir3.Block toBlock : b.outgoing) {
                toBlock.incoming.add(b);
            }
        }

        method.blocks = blocks;
        createIterators(method);
    }

    private void createIterators(Ir3.Method method) {
        assert method.blocks != null;
        postOrderIndex = 0;
        preOrderIndex = 0;
        method.blockPreOrder = new ArrayList<>();
        method.blockPostOrder = new ArrayList<>();
        HashSet<Ir3.Block> visited = new HashSet<>();
        dfs(method.blocks.get(0), visited, method);
    }

    private void dfs(Ir3.Block block, HashSet<Ir3.Block> visited, Ir3.Method method) {
        visited.add(block);
        method.blockPreOrder.add(block);
        block.preOrderIndex = preOrderIndex++;

        for (Ir3.Block b : block.outgoing) {
            if (!visited.contains(b)) {
                dfs(b, visited, method);
            }
        }
        method.blockPostOrder.add(block);
        block.postOrderIndex = postOrderIndex++;
    }
}
