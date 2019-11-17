package jlite.pass;

import com.google.common.collect.Lists;
import jlite.arm.Arm;
import jlite.ir.Ir3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ArmGenPass {
    private HashMap<String, Integer> fieldOffsets = new HashMap<>();
    private ArrayList<Arm.Block> text = new ArrayList<>();
    private ArrayList<Arm.Block> data = new ArrayList<>();
    private ArrayList<String> globals = new ArrayList<>();
    private HashMap<Ir3.Block, String> blockLabelMap;
    private HashSet<Arm.Reg> calleeRegisters;
    private boolean isMain;
    private HashMap<Ir3.Var, Integer> stackOffsets = new HashMap<>();
    private LabelGenerator labelGenerator = new LabelGenerator();
    private Arm.Block currBlock;

    public Arm.Prog pass(Ir3.Prog prog) {
        for (Ir3.Data data : prog.datas) {
            HashMap<String, Integer> offsets = new HashMap<>();
            int offset = 0;
            for (Ir3.DataField dataField : data.fields) {
                offsets.put(data.cname, offset);
                offset += 4;
            }
            fieldOffsets = offsets;
        }
        globals.add("main");

        for (Ir3.Method method : prog.methods) {
            passMeth(method);
        }

        return new Arm.Prog(text, data, globals);
    }

    private void passMeth(Ir3.Method method) {
        Arm.Block prologue = new Arm.Block(getLabel(method), new ArrayList<>());
        text.add(prologue);
        prologue.isPrologue = true;
        currBlock = prologue;
        isMain = method.name.equals("main");

        blockLabelMap = new HashMap<>();
        for (Ir3.Block block : method.blocks) {
            blockLabelMap.put(block, labelGenerator.gen());
        }

        calleeRegisters = new HashSet<>();
        for (Ir3.Var var : method.locals) {
            if (var.reg >= 0) {
                calleeRegisters.add(toReg(var));
            }
        }
        calleeRegisters.remove(Arm.Reg.R0);
        calleeRegisters.remove(Arm.Reg.R1);
        calleeRegisters.remove(Arm.Reg.R2);
        calleeRegisters.remove(Arm.Reg.R3);

        // Allocate stack
        HashSet<Ir3.Var> requiresStack = new HashSet<>();

        for (Ir3.Block block : method.blocks) {
            for (Ir3.Stmt stmt : block.statements) {
                if (stmt instanceof Ir3.StackArgStmt) {
                    Ir3.StackArgStmt stackArgStmt = (Ir3.StackArgStmt) stmt;
                    requiresStack.add(stackArgStmt.var);
                } else if (stmt instanceof Ir3.StoreStmt) {
                    Ir3.StoreStmt storeStmt = (Ir3.StoreStmt) stmt;
                    requiresStack.add(storeStmt.var);
                }
            }
        }

        int stackSize = requiresStack.size() * 4;
        int stackOffset = 0;
        stackOffsets = new HashMap<>();
        for (Ir3.Var var : requiresStack) {
            stackOffsets.put(var, stackOffset);
            stackOffset += 4;
        }

        if (!calleeRegisters.isEmpty()) {
            currBlock.armIsns.add(new Arm.PushIsn(calleeRegisters));
        }

        currBlock.armIsns.add(new Arm.SubIsn(Arm.Reg.SP, Arm.Reg.SP, new Arm.Op2Const(stackSize)));

        String epilogueLabel = labelGenerator.gen();
        if (method.blockPostOrder.isEmpty()) {
            currBlock.armIsns.add(new Arm.BIsn(epilogueLabel));
        } else {
            currBlock.armIsns.add(new Arm.BIsn(blockLabelMap.get(method.blockPostOrder.get(method.blockPostOrder.size() - 1))));
        }

        List<Ir3.Block> reversePo = Lists.reverse(method.blockPostOrder);
        for (Ir3.Block block : reversePo) {
            doBlock(block);
        }

        Arm.Block epilogueBlock = new Arm.Block(epilogueLabel, new ArrayList<>());
        currBlock = epilogueBlock;
        currBlock.armIsns.add(new Arm.AddIsn(Arm.Reg.SP, Arm.Reg.SP, new Arm.Op2Const(stackSize)));

        if (calleeRegisters.contains(Arm.Reg.LR)) {
            if (isMain) doAssign(Arm.Reg.R0, 0);
            calleeRegisters.remove(Arm.Reg.LR);
            calleeRegisters.add(Arm.Reg.PC);
            currBlock.armIsns.add(new Arm.PopIsn(calleeRegisters));
        } else {
            if (!calleeRegisters.isEmpty()) {
                currBlock.armIsns.add(new Arm.PopIsn(calleeRegisters));
            }
            if (isMain) doAssign(Arm.Reg.R0, 0);
            currBlock.armIsns.add(new Arm.BxIsn(Arm.Reg.LR));
        }
    }

    private void doAssign(Arm.Reg reg, int i) {
        currBlock.armIsns.add(new Arm.LdrConstIsn(reg, i));
    }

    private void doBlock(Ir3.Block block) {
    }

    private Arm.Reg toReg(Ir3.Var var) {
        assert (var.reg >= 0);
        return Arm.Reg.fromInt(var.reg);
    }

    private String getLabel(Ir3.Method method) {
        if (method.name == "main") return method.name;
        return "." + method.name.replace("%", "__");
    }

    private class LabelGenerator {
        private int counter = 0;

        public String gen() {
            return ".L" + counter++;
        }
    }
}
