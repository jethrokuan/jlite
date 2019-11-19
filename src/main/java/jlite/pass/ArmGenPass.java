package jlite.pass;

import com.google.common.collect.Lists;
import jlite.arm.Arm;
import jlite.ir.Ir3;
import jlite.parser.Ast;

import java.util.*;

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
    String epilogueLabel;
    private HashMap<String, String> stringLabelMap = new HashMap<>();

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
                } else if (stmt instanceof Ir3.LoadStmt) {
                    Ir3.LoadStmt loadStmt = (Ir3.LoadStmt) stmt;
                    requiresStack.add(loadStmt.var);
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

        epilogueLabel = labelGenerator.gen();
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

        if (isMain) doAssign(Arm.Reg.R0, 0);

        if (calleeRegisters.contains(Arm.Reg.LR)) {
            calleeRegisters.remove(Arm.Reg.LR);
            calleeRegisters.add(Arm.Reg.PC);
            currBlock.armIsns.add(new Arm.PopIsn(calleeRegisters));
        } else {
            if (!calleeRegisters.isEmpty()) {
                currBlock.armIsns.add(new Arm.PopIsn(calleeRegisters));
            }
            currBlock.armIsns.add(new Arm.BxIsn(Arm.Reg.LR));
        }
        text.add(epilogueBlock);
    }

    private void doAssign(Arm.Reg reg, int i) {
        currBlock.armIsns.add(new Arm.LdrConstIsn(reg, i));
    }

    private void doAssign(Arm.Reg r1, Arm.Reg r2) {
        if (r1 != r2) currBlock.armIsns.add(new Arm.MovIsn(r1, new Arm.Op2Reg(r2)));
    }

    private void doAssign(Arm.Reg reg, Ir3.Rval rv) {
        if (rv instanceof Ir3.StringRval) {
            Ir3.StringRval stringRval = (Ir3.StringRval) rv;
            if (!stringLabelMap.containsKey(stringRval.s)) {
                String label = labelGenerator.gen();
                Arm.AscizIsn ascizIsn = new Arm.AscizIsn(stringRval.s);
                List<Arm.ArmIsn> isns = Arrays.asList(ascizIsn);
                Arm.Block strBlock = new Arm.Block(label, isns);
                data.add(strBlock);
                stringLabelMap.put(stringRval.s, label);
            }
            String label = stringLabelMap.get(stringRval.s);
            currBlock.armIsns.add(new Arm.LdrLabelIsn(reg, label));
        } else if (rv instanceof Ir3.VarRval) {
            doAssign(reg, toReg(rv));
        } else {
            int i = Arm.toInt(rv);
            doAssign(reg, i);
        }
    }

    private void doBlock(Ir3.Block block) {
        currBlock = new Arm.Block(blockLabelMap.get(block), new ArrayList<>());
        for (Ir3.Stmt stmt : block.statements) {
            doStmt(block, stmt);
        }
        text.add(currBlock);
    }

    private void doStmt(Ir3.Block block, Ir3.Stmt stmt) {
        if (stmt instanceof Ir3.CmpStmt) {
            Ir3.CmpStmt cmpStmt = (Ir3.CmpStmt) stmt;
            String targetLabel = blockLabelMap.get(block.outgoing.get(0));
            String fallthroughLabel = blockLabelMap.get(block.outgoing.get(1));

            Arm.Reg lhs = toReg(cmpStmt.lRv);
            Arm.Op2 rhs = toOp2(cmpStmt.rRv);

            currBlock.armIsns.add(new Arm.CmpIsn(lhs, rhs));
            currBlock.armIsns.add(new Arm.BIsn(opToCond(cmpStmt.op), targetLabel));
            currBlock.armIsns.add(new Arm.BIsn(fallthroughLabel));
        } else if (stmt instanceof Ir3.GotoStmt) {
            String target = blockLabelMap.get(block.outgoing.get(0));
            currBlock.armIsns.add(new Arm.BIsn(target));
        } else if (stmt instanceof Ir3.BinaryStmt) {
            Ir3.BinaryStmt binaryStmt = (Ir3.BinaryStmt) stmt;
            Arm.Reg dst = toReg(binaryStmt.dst);

            if (Arm.isConstant(binaryStmt.lhs) && Arm.isConstant(binaryStmt.rhs)) {
                int lhs = Arm.toInt(binaryStmt.lhs);
                int rhs = Arm.toInt(binaryStmt.rhs);
                int res;

                switch (binaryStmt.op) {
                    case PLUS:
                        res = lhs + rhs;
                        break;
                    case MINUS:
                        res = lhs - rhs;
                        break;
                    case MULT:
                        res = lhs * rhs;
                        break;
                    case DIV:
                        res = lhs / rhs;
                        break;
                    default:
                        throw new AssertionError("WAT");
                }
                doAssign(dst, res);
            } else if (binaryStmt.op == Ast.BinaryOp.MULT) {
                // MULT requires both to be reg
                Arm.Reg lhs = toReg(binaryStmt.lhs);
                Arm.Reg rhs = toReg(binaryStmt.rhs);
                currBlock.armIsns.add(new Arm.MulIsn(dst, lhs, rhs));
            } else if (binaryStmt.op == Ast.BinaryOp.PLUS || binaryStmt.op == Ast.BinaryOp.MINUS) {
                Arm.Reg lhs = toReg(binaryStmt.lhs);
                Arm.Op2 rhs = toOp2(binaryStmt.rhs);
                if (binaryStmt.op == Ast.BinaryOp.PLUS) {
                    currBlock.armIsns.add(new Arm.AddIsn(dst, lhs, rhs));
                } else {
                    currBlock.armIsns.add(new Arm.SubIsn(dst, lhs, rhs));
                }
            } else {
                throw new AssertionError("invalid binarystmt op " + binaryStmt.op);
            }
        } else if (stmt instanceof Ir3.UnaryStmt) {
            Ir3.UnaryStmt unaryStmt = (Ir3.UnaryStmt) stmt;
            Arm.Reg dst = toReg(unaryStmt.dst);

            if (Arm.isConstant(unaryStmt.rv)) {
                int v = Arm.toInt(unaryStmt.rv);
                if (unaryStmt.op != Ast.UnaryOp.NEGATIVE) throw new AssertionError("invalid unary op " + unaryStmt.op);
                doAssign(dst, -v);
            } else if (unaryStmt.op == Ast.UnaryOp.NEGATIVE) {
                Arm.Reg rhs = toReg(unaryStmt.rv);
                currBlock.armIsns.add(new Arm.RsbIsn(dst, rhs, new Arm.Op2Const(0)));
            }
        } else if (stmt instanceof Ir3.AssignStmt) {
            Arm.Reg lhs = toReg(((Ir3.AssignStmt) stmt).var);
            doAssign(lhs, ((Ir3.AssignStmt) stmt).rval);
        } else if (stmt instanceof Ir3.ReturnStmt) {
            Ir3.ReturnStmt returnStmt = (Ir3.ReturnStmt) stmt;
            if (returnStmt.rv != null) {
                doAssign(Arm.Reg.R0, returnStmt.rv);
            }

            currBlock.armIsns.add(new Arm.BIsn(epilogueLabel));
        } else {
            throw new AssertionError("unsupported stmt type " + stmt.getClass().toString());
        }
    }

    private Arm.Cond opToCond(Ast.BinaryOp op) {
        switch (op) {
            case LT:
                return Arm.Cond.LT;
            case GT:
                return Arm.Cond.GT;
            case LEQ:
                return Arm.Cond.LE;
            case GEQ:
                return Arm.Cond.GE;
            case NEQ:
                return Arm.Cond.NE;
            case EQ:
                return Arm.Cond.EQ;
            default:
                throw new AssertionError("Invalid cond op: " + op.toString());
        }
    }

    private Arm.Op2 toOp2(Ir3.Rval rv) {
        if (Arm.isConstant(rv)) {
            return new Arm.Op2Const(rv);
        } else if (rv instanceof Ir3.VarRval) {
            return new Arm.Op2Reg(toReg(rv));
        } else {
            throw new AssertionError("bad rv " + rv.print());
        }
    }

    private Arm.Reg toReg(Ir3.Rval rv) {
        return toReg(((Ir3.VarRval) rv).var);
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
