package jlite.pass;

import jlite.arm.Arm;
import jlite.ir.Ir3;
import jlite.parser.Ast;

import java.util.ArrayList;

public class LowerPass {
    ArrayList<Ir3.Stmt> newStmts = new ArrayList<>();
    private Ir3.Method method;
    ArrayList<Ir3.Var> stackVars = new ArrayList<>();
    private TempGenerator tempGenerator = new TempGenerator();

    public void pass(Ir3.Prog prog) {
        for (Ir3.Method method : prog.methods) {
            pass(method);
        }
    }

    private void pass(Ir3.Method method) {
        this.method = method;
        stackVars = new ArrayList<>();
        for (int i = 4; i < method.args.size(); i++) {
            stackVars.add(method.args.get(i));
        }

        for (Ir3.Block block : method.blocks) {
            newStmts = new ArrayList<>();
            for (Ir3.Stmt stmt : block.statements) {
                passStmt(stmt);
            }
            block.statements = newStmts;
        }
    }

    private void passStmt(Ir3.Stmt stmt) {
        for (Ir3.Var use : stmt.getUses()) {
            if (stackVars.contains(use)) {
                newStmts.add(new Ir3.LoadStmt(use));
            }
        }

        if (stmt instanceof Ir3.CallStmt) {
            Ir3.CallStmt callStmt = (Ir3.CallStmt) stmt;

            for (int i = 0; i < callStmt.args.size(); i++) {
                Ir3.Rval rv = callStmt.args.get(i);
                if (rv instanceof Ir3.VarRval) continue;
                Ir3.Var var = tempGenerator.gen(rv.getTyp());
                passStmt(new Ir3.AssignStmt(var, rv));
                callStmt.args.set(i, new Ir3.VarRval(var));
            }

            for (int i = 0; i < callStmt.args.size() && i < 4; i++) {
                // We are not guaranteed that these R0-R3 are restored, so we store them
                Ir3.Var var = ((Ir3.VarRval) callStmt.args.get(i)).var;
                newStmts.add(new Ir3.StoreStmt(var));
            }

            for (int i = callStmt.args.size() - 1; i >= 4; i--) {
                Ir3.Rval arg = callStmt.args.get(i);
                Ir3.Var v;
                if (!(arg instanceof Ir3.VarRval)) {
                    v = tempGenerator.gen(arg.getTyp());
                    passStmt(new Ir3.AssignStmt(v, arg));
                } else {
                    Ir3.VarRval varRval = (Ir3.VarRval) arg;
                    v = varRval.var;
                }
                passStmt(new Ir3.StackArgStmt(v, i - 4));
                callStmt.args.remove(i);
            }

            if (callStmt.lhs == null) {
                callStmt.lhs = tempGenerator.gen(new Ast.VoidTyp());
            }

            newStmts.add(callStmt);

            for (int i = 0; i < callStmt.args.size() && i < 4; i++) {
                // We are not guaranteed that these R0-R3 are restored, so we restore them
                Ir3.Var var = ((Ir3.VarRval) callStmt.args.get(i)).var;
                newStmts.add(new Ir3.LoadStmt(var));
            }

            return;
        } else if (stmt instanceof Ir3.CmpStmt) {
            Ir3.CmpStmt cmpStmt = (Ir3.CmpStmt) stmt;

            if (Arm.isConstant(cmpStmt.lRv) && Arm.isConstant(cmpStmt.rRv)) {
                newStmts.add(cmpStmt);
                return;
            }

            if (!(cmpStmt.lRv instanceof Ir3.VarRval)) {
                Ir3.Var temp = tempGenerator.gen(cmpStmt.lRv.getTyp());
                passStmt(new Ir3.AssignStmt(temp, cmpStmt.lRv));
                cmpStmt.lRv = new Ir3.VarRval(temp);
            }

            if (!Arm.isValidOp2(cmpStmt.rRv) && !(cmpStmt.rRv instanceof Ir3.VarRval)) {
                Ir3.Var temp = tempGenerator.gen(cmpStmt.rRv.getTyp());
                passStmt(new Ir3.AssignStmt(temp, cmpStmt.rRv));
                cmpStmt.rRv = new Ir3.VarRval(temp);
            }
            newStmts.add(cmpStmt);
        } else if (stmt instanceof Ir3.PrintlnStmt) {
            Ir3.PrintlnStmt printlnStmt = (Ir3.PrintlnStmt) stmt;
            ArrayList<Ir3.Rval> args = new ArrayList<>();
            if (printlnStmt.rval.getTyp().isSubTypeOrEquals(new Ast.IntTyp())) {
                args.add(new Ir3.StringRval("%i"));
                args.add(printlnStmt.rval);
                passStmt(new Ir3.PrintfStmt(args));
                return;
            } else {
                args.add(printlnStmt.rval);
                passStmt(new Ir3.PrintfStmt(args));
                return;
            }
        } else if (stmt instanceof Ir3.UnaryStmt) {
            Ir3.UnaryStmt unaryStmt = (Ir3.UnaryStmt) stmt;
            if (Arm.isConstant(unaryStmt.rv)) {
                newStmts.add(stmt);
                return;
            }
            if (!(unaryStmt.rv instanceof Ir3.VarRval)) {
                Ir3.Var temp = tempGenerator.gen(unaryStmt.rv.getTyp());
                passStmt(new Ir3.AssignStmt(temp, unaryStmt.rv));
                unaryStmt.rv = new Ir3.VarRval(temp);
            }
            newStmts.add(stmt);
            return;
        } else if (stmt instanceof Ir3.FieldAssignStatement) {
            Ir3.FieldAssignStatement fieldAssignStatement = (Ir3.FieldAssignStatement) stmt;
            if (!(fieldAssignStatement.v instanceof Ir3.VarRval)) {
                Ir3.Var temp = tempGenerator.gen(fieldAssignStatement.v.getTyp());
                passStmt(new Ir3.AssignStmt(temp, fieldAssignStatement.v));
                fieldAssignStatement.v = new Ir3.VarRval(temp);
                return;
            }
        } else if (stmt instanceof Ir3.FieldAccessStatement) {
            newStmts.add(stmt);
            return;
        } else if (stmt instanceof Ir3.BinaryStmt) {
            Ir3.BinaryStmt binaryStmt = (Ir3.BinaryStmt) stmt;

            if (binaryStmt.op == Ast.BinaryOp.DIV) throw new AssertionError("Division not supported");
            if (!(binaryStmt.lhs instanceof Ir3.VarRval)) {
                Ir3.Var temp = tempGenerator.gen(binaryStmt.lhs.getTyp());
                passStmt(new Ir3.AssignStmt(temp, binaryStmt.lhs));
                binaryStmt.lhs = new Ir3.VarRval(temp);
            }

            if (!(binaryStmt.rhs instanceof Ir3.VarRval)) {
                Ir3.Var temp = tempGenerator.gen(binaryStmt.rhs.getTyp());
                passStmt(new Ir3.AssignStmt(temp, binaryStmt.rhs));
                binaryStmt.rhs = new Ir3.VarRval(temp);
            }
            newStmts.add(stmt);
            return;
        } else if (stmt instanceof Ir3.StackArgStmt) {
            newStmts.add(stmt);
            return;
        } else if (stmt instanceof Ir3.ReturnStmt) {
            newStmts.add(stmt);
            return;
        } else if (stmt instanceof Ir3.PrintfStmt) {
            newStmts.add(stmt);
            return;
        } else {
            newStmts.add(stmt);
            return;
        }
    }

    private class TempGenerator {
        public int counter = 0;

        public Ir3.Var gen(Ast.Typ typ) {
            String name = "%t" + counter++;
            Ir3.Var v = new Ir3.Var(typ, name);
            method.locals.add(v);
            return v;
        }
    }
}
