package jlite.ir;

import jlite.StaticChecker;
import jlite.parser.Ast;
import jlite.parser.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Ir3Gen {

    TempGenerator tempGenerator = new TempGenerator();
    LabelGenerator labelGenerator = new LabelGenerator();
    private ArrayList<Ir3.Data> datas = new ArrayList<>();
    private HashMap<String, Ir3.Data> dataMap = new HashMap<>();
    private ArrayList<Ir3.Method> methods = new ArrayList<>();
    private HashMap<Ast.MdDecl, Ir3.Method> methodMap = new HashMap<>();

    public static void main(String[] argv) {
        Arrays.stream(argv).forEach(fileLoc -> {
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                Ir3Gen ir3Gen = new Ir3Gen();
                Ir3.Prog ir3 = ir3Gen.gen(prog);
                System.out.println(ir3.print());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Ir3.Prog gen(Ast.Prog prog) {
        for (Ast.Clas clas : prog.clasList) {
            ArrayList<Ir3.DataField> fields = new ArrayList<>();
            for (Ast.VarDecl varDecl : clas.varDeclList) {
                Ir3.DataField field = new Ir3.DataField(varDecl.type, varDecl.ident);
                fields.add(field);
            }
            Ir3.Data data = new Ir3.Data(clas.cname, fields);
            datas.add(data);
            dataMap.put(clas.cname, data);
        }

        for (Ast.Clas clas : prog.clasList) {
            HashMap<String, Integer> nameCounter = new HashMap<>();
            for (Ast.MdDecl mdDecl : clas.mdDeclList) {
                String name;
                if (mdDecl.name.equals("main")) {
                    name = mdDecl.name;
                } else {
                    Integer count = nameCounter.getOrDefault(mdDecl.name, 0);
                    name = "%" + clas.cname + "_" + mdDecl.name + "_" + count;
                    nameCounter.put(mdDecl.name, count + 1);
                }
                methodMap.put(mdDecl, new Ir3.Method(name, mdDecl.retTyp));
            }
        }

        for (Ast.Clas clas : prog.clasList) {
            genClas(clas);
        }

        return new Ir3.Prog(datas, methods);
    }

    private void genClas(Ast.Clas clas) {
        for (Ast.MdDecl mdDecl : clas.mdDeclList) {
            Ir3.Method method = methodMap.get(mdDecl);
            methods.add(method);

            HashMap<String, Integer> nameCounter = new HashMap<>();

            ArrayList<Ir3.Var> args = new ArrayList<>();
            ArrayList<Ir3.Var> locals = new ArrayList<>();
            HashMap<Ast.VarDecl, Ir3.Var> varMap = new HashMap<>();

            // generate args
            Ir3.Var thisVar = new Ir3.Var(new Ast.ClasTyp(clas.cname), "this");
            args.add(thisVar);
            for (Ast.VarDecl arg : mdDecl.args) {
                Ir3.Var var = new Ir3.Var(arg.type, arg.ident);
                args.add(var);
                varMap.put(arg, var);
                nameCounter.put(arg.ident, 1); // We can assume unique
            }

            for (Ast.VarDecl v : mdDecl.vars) {
                String name;
                Integer count = nameCounter.getOrDefault(v.ident, 0);
                if (count == 0) {
                    name = v.ident;
                    nameCounter.put(v.ident, 1);
                } else {
                    name = v.ident + "__" + count;
                    nameCounter.put(v.ident, count + 1);
                }

                Ir3.Var var = new Ir3.Var(v.type, name);
                locals.add(var);
                varMap.put(v, var);
            }

            StmtChunk chunk = genStmts(mdDecl.stmts, method);
            ArrayList<Ir3.Stmt> irStmts = new ArrayList<>(chunk.stmts);

            if (!chunk.nextjumps.isEmpty()) {
                Ir3.LabelStmt label = labelGenerator.gen();
                irStmts.add(label);
                backpatch(chunk.nextjumps, label);
            }

            method.args.addAll(args);
            method.locals.addAll(locals);
            method.statements.addAll(irStmts);
        }
    }

    private void backpatch(ArrayList<Ir3.JumpStmt> jumps, Ir3.LabelStmt label) {
        for (Ir3.JumpStmt jump : jumps) {
            jump.setLabel(label);
        }
    }

    private StmtChunk genStmts(List<Ast.Stmt> stmts, Ir3.Method method) {
        ArrayList<Ir3.Stmt> irStmts = new ArrayList<>();
        ArrayList<Ir3.JumpStmt> nextjumps;

        StmtChunk prev = null;
        for (Ast.Stmt stmt : stmts) {
            StmtChunk chunk = genStmt(stmt, method);
            if (prev != null && !prev.nextjumps.isEmpty()) {
                Ir3.LabelStmt label = labelGenerator.gen();
                irStmts.add(label);
                backpatch(prev.nextjumps, label);
            }
            irStmts.addAll(chunk.stmts);
            prev = chunk;
        }

        nextjumps = prev != null ? prev.nextjumps : new ArrayList<>();

        return new StmtChunk(irStmts, nextjumps);
    }

    private StmtChunk genStmt(Ast.Stmt stmt, Ir3.Method method) {
        ArrayList<Ir3.Stmt> statementList = new ArrayList<>();
        ArrayList<Ir3.JumpStmt> nextjumps = new ArrayList<>();

        if (stmt instanceof Ast.ReadlnStmt) {
            Ast.ReadlnStmt readlnStmt = (Ast.ReadlnStmt) stmt;
            statementList.add(new Ir3.ReadlnStmt(readlnStmt.ident));
            return new StmtChunk(statementList, nextjumps);
        } else if (stmt instanceof Ast.PrintlnStmt) {
            Ast.PrintlnStmt printlnStmt = (Ast.PrintlnStmt) stmt;
            RvalRes res = doRval(printlnStmt.expr, method);
            statementList.addAll(res.statements);
            statementList.add(new Ir3.PrintlnStmt((Ir3.Var) res.rv));
            return new StmtChunk(statementList, nextjumps);
        } else if (stmt instanceof Ast.VarAssignStmt) {
            Ast.VarAssignStmt varAssignStmt = (Ast.VarAssignStmt) stmt;
            RvalRes res = doRval(varAssignStmt.rhs, method);
            statementList.addAll(res.statements);
            Ir3.Var var = new Ir3.Var(res.rv.getTyp(), varAssignStmt.lhs); // Hack: we don't care about type
            statementList.add(new Ir3.AssignStmt(var, res.rv));
            return new StmtChunk(statementList, nextjumps);
        } else if (stmt instanceof Ast.ReturnStmt) {
            Ast.ReturnStmt returnStmt = (Ast.ReturnStmt) stmt;
            Ir3.Rval rv = null;
            if (returnStmt.expr != null) {
                RvalRes res = doRval(returnStmt.expr, method);
                rv = res.rv;
                statementList.addAll(res.statements);
            }
            statementList.add(new Ir3.ReturnStmt(rv));
            return new StmtChunk(statementList, nextjumps);
        } else if (stmt instanceof Ast.FieldAssignStmt) {
            return new StmtChunk(statementList, nextjumps);
        } else if (stmt instanceof Ast.CallStmt) {
            Ast.CallStmt callStmt = (Ast.CallStmt) stmt;
            return new StmtChunk(statementList, nextjumps);
        } else if (stmt instanceof Ast.IfStmt) {
            Ast.IfStmt ifStmt = (Ast.IfStmt) stmt;
            CondChunk cond = doCond(ifStmt.cond, method);
            StmtChunk thenChunk = genStmts(ifStmt.thenStmtList, method);
            StmtChunk elseChunk = genStmts(ifStmt.elseStmtList, method);

            statementList.addAll(cond.statements);

            if (!cond.truejumps.isEmpty()) {
                Ir3.LabelStmt trueLabel = labelGenerator.gen();
                statementList.add(trueLabel);
                backpatch(cond.truejumps, trueLabel);
            }

            statementList.addAll(thenChunk.stmts);
            nextjumps.addAll(thenChunk.nextjumps);

            if (isFallThrough(thenChunk.stmts)) {
                Ir3.GotoStmt thenGoto = new Ir3.GotoStmt();
                statementList.add(thenGoto);
                nextjumps.add(thenGoto);
            }

            if (!cond.falsejumps.isEmpty()) {
                Ir3.LabelStmt falseLabel = labelGenerator.gen();
                statementList.add(falseLabel);
                backpatch(cond.falsejumps, falseLabel);
            }

            statementList.addAll(elseChunk.stmts);
            nextjumps.addAll(elseChunk.nextjumps);
        } else if (stmt instanceof Ast.WhileStmt) {
            Ast.Expr cond = ((Ast.WhileStmt) stmt).cond;
            Ast.WhileStmt whileStmt = (Ast.WhileStmt) stmt;

            CondChunk condChunk = doCond(cond, method);
            StmtChunk stmtChunk = genStmts(whileStmt.stmtList, method);

            Ir3.LabelStmt topLabel = labelGenerator.gen();
            statementList.add(topLabel);
            statementList.addAll(condChunk.statements);

            if (!condChunk.truejumps.isEmpty()) {
                Ir3.LabelStmt trueLabel = labelGenerator.gen();
                statementList.add(trueLabel);
                backpatch(condChunk.truejumps, trueLabel);
            }

            // Loop body
            statementList.addAll(stmtChunk.stmts);
            if (isFallThrough(stmtChunk.stmts))
                statementList.add(new Ir3.GotoStmt(topLabel));

            nextjumps.addAll(condChunk.falsejumps);
            nextjumps.addAll(stmtChunk.nextjumps);

            return new StmtChunk(statementList, nextjumps);
        } else {
            System.out.println("Unhandled stmt type:" + stmt.getClass().toString());
        }
        return new StmtChunk(statementList, nextjumps);
    }

    private boolean isFallThrough(ArrayList<Ir3.Stmt> stmts) {
        if (stmts.isEmpty()) {
            return true;
        }
        Ir3.Stmt last = stmts.get(stmts.size() - 1);
        return !(last instanceof Ir3.ReturnStmt || last instanceof Ir3.GotoStmt);
    }

    private RvalRes doRval(Ast.Expr expr, Ir3.Method method) {
        ArrayList<Ir3.Stmt> statementList = new ArrayList<>();
        if (expr instanceof Ast.IntLitExpr) {
            Ir3.Var v = tempGenerator.gen(new Ast.IntTyp(), method);
            statementList.add(new Ir3.AssignStmt(v, ((Ast.IntLitExpr) expr).val));
            return new RvalRes(v, statementList);
        } else if (expr instanceof Ast.StringLitExpr) {
            Ir3.Var v = tempGenerator.gen(new Ast.StringTyp(), method);
            statementList.add(new Ir3.AssignStmt(v, ((Ast.StringLitExpr) expr).str));
            return new RvalRes(v, statementList);
        } else if (expr instanceof Ast.BoolLitExpr) {
            Ir3.Var v = tempGenerator.gen(new Ast.BoolTyp(), method);
            statementList.add(new Ir3.AssignStmt(v, ((Ast.BoolLitExpr) expr).val));
            return new RvalRes(v, statementList);
        } else if (expr instanceof Ast.IdentExpr) {
            Ir3.Var v = new Ir3.Var(expr.typ, ((Ast.IdentExpr) expr).ident);
            return new RvalRes(v, statementList);
        } else if (expr instanceof Ast.ThisExpr) {
            Ir3.Var v = new Ir3.Var(expr.typ, "this");
            return new RvalRes(v, statementList);
        } else if (expr instanceof Ast.BinaryExpr) {
            Ast.BinaryExpr binaryExpr = (Ast.BinaryExpr) expr;
            switch (binaryExpr.op) {
                case LT:
                case GT:
                case GEQ:
                case LEQ:
                case NEQ:
                case EQ:
                case AND:
                case OR: {
                    CondChunk cond = doCond(expr, method);
                    Ir3.Var v = tempGenerator.gen(new Ast.BoolTyp(), method);
                    Ir3.LabelStmt trueLabel = null;
                    Ir3.LabelStmt falseLabel = null;

                    if (!cond.truejumps.isEmpty()) {
                        trueLabel = labelGenerator.gen();
                        backpatch(cond.truejumps, trueLabel);
                    }

                    if (!cond.falsejumps.isEmpty()) {
                        falseLabel = labelGenerator.gen();
                        backpatch(cond.falsejumps, falseLabel);
                    }

                    statementList.addAll(cond.statements);

                    if (trueLabel != null && falseLabel != null) {
                        // L_true:
                        //   t = true
                        //   goto L_end
                        // L_false:
                        //   t = false
                        //   goto L_end
                        // L_end
                        Ir3.LabelStmt restLabel = labelGenerator.gen();
                        statementList.add(trueLabel);
                        statementList.add(new Ir3.AssignStmt(v, true));
                        statementList.add(new Ir3.GotoStmt(restLabel));
                        statementList.add(falseLabel);
                        statementList.add(new Ir3.AssignStmt(v, false));
                        statementList.add(restLabel);
                    } else if (trueLabel != null) {
                        // L_true:
                        // t = true
                        statementList.add(trueLabel);
                        statementList.add(new Ir3.AssignStmt(v, true));
                    } else if (falseLabel != null) {
                        // L_false:
                        // t = false
                        statementList.add(falseLabel);
                        statementList.add(new Ir3.AssignStmt(v, false));
                    }

                    return new RvalRes(v, statementList);
                }
                case PLUS:
                case MINUS:
                case DIV:
                case MULT: {
                    // 1. Evaluate LHS = t1
                    // 2. Evaluate RHS = t2
                    // S(LHS) ++ S(RHS) ++ [t3 = t1 op t2]
                    RvalRes lRes = doRval(binaryExpr.lhs, method);
                    RvalRes rRes = doRval(binaryExpr.rhs, method);
                    Ir3.Var v = tempGenerator.gen(binaryExpr.typ, method);
                    statementList.addAll(lRes.statements);
                    statementList.addAll(rRes.statements);
                    Ir3.Expr3 binaryExpr3 = new Ir3.BinaryExpr(binaryExpr.op, lRes.rv, rRes.rv);
                    statementList.add(new Ir3.AssignStmt(v, binaryExpr3));

                    return new RvalRes(v, statementList);
                }
                default:
                    System.out.println("oops");
            }
        } else if (expr instanceof Ast.NewExpr) {
            Ast.NewExpr newExpr = (Ast.NewExpr) expr;
            String cname = newExpr.cname;
            Ir3.Var temp = tempGenerator.gen(newExpr.typ, method);
            Ir3.NewExpr newExpr3 = new Ir3.NewExpr(dataMap.get(cname));
            Ir3.AssignStmt assignStmt = new Ir3.AssignStmt(temp, newExpr3);
            statementList.add(assignStmt);
            return new RvalRes(temp, statementList);
        } else {
            System.out.println("Unhandled expr type: " + expr.getClass().toString());
        }
        return new RvalRes(null, new ArrayList<>());
    }

    private CondChunk doCond(Ast.Expr expr, Ir3.Method method) {
        ArrayList<Ir3.Stmt> statements = new ArrayList<>();
        ArrayList<Ir3.JumpStmt> truejumps = new ArrayList<>();
        ArrayList<Ir3.JumpStmt> falsejumps = new ArrayList<>();

        if (expr instanceof Ast.IntLitExpr) {
            Ast.IntLitExpr intLitExpr = (Ast.IntLitExpr) expr;
            Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt();
            statements.add(gotoStmt);

            if (intLitExpr.val == 0) {
                falsejumps.add(gotoStmt);
            } else {
                truejumps.add(gotoStmt);
            }

            return new CondChunk(statements, truejumps, falsejumps);
        } else if (expr instanceof Ast.BoolLitExpr) {
            Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt();
            Ast.BoolLitExpr boolLitExpr = (Ast.BoolLitExpr) expr;
            statements.add(gotoStmt);

            if (boolLitExpr.val) {
                truejumps.add(gotoStmt);
            } else {
                falsejumps.add(gotoStmt);
            }

            return new CondChunk(statements, truejumps, falsejumps);
        } else if (expr instanceof Ast.NullLitExpr) {
            Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt();
            statements.add(gotoStmt);
            falsejumps.add(gotoStmt);
            return new CondChunk(statements, truejumps, falsejumps);
        } else if (expr instanceof Ast.ThisExpr || expr instanceof Ast.StringLitExpr) {
            Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt();
            statements.add(gotoStmt);
            truejumps.add(gotoStmt);
            return new CondChunk(statements, truejumps, falsejumps);
        } else if (expr instanceof Ast.UnaryExpr) {
            Ast.UnaryOp op = ((Ast.UnaryExpr) expr).op;
            Ast.Expr exprExpr = ((Ast.UnaryExpr) expr).expr;

            switch (op) {
                case NOT:
                    CondChunk chunk = doCond(exprExpr, method);
                    statements.addAll(chunk.statements);
                    truejumps.addAll(chunk.falsejumps);
                    falsejumps.addAll(chunk.truejumps);
                    return new CondChunk(statements, truejumps, falsejumps);
                case NEGATIVE:
                    RvalRes res = doRval(exprExpr, method);
                    statements.addAll(res.statements);
                    Ir3.IntConst zero = new Ir3.IntConst(0);
                    Ir3.CmpStmt tstStmt = new Ir3.CmpStmt(Ast.BinaryOp.NEQ, res.rv, zero);
                    statements.add(tstStmt);
                    truejumps.add(tstStmt);
                    Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt();
                    statements.add(gotoStmt);
                    falsejumps.add(gotoStmt);
                    return new CondChunk(statements, truejumps, falsejumps);
            }
        } else if (expr instanceof Ast.BinaryExpr) {
            Ast.BinaryExpr binaryExpr = (Ast.BinaryExpr) expr;
            Ast.BinaryOp op = binaryExpr.op;
            Ast.Expr lhs = binaryExpr.lhs;
            Ast.Expr rhs = binaryExpr.rhs;

            switch (op) {
                case AND: { // &&
                    CondChunk lhsChunk = doCond(lhs, method);
                    CondChunk rhsChunk = doCond(rhs, method);
                    Ir3.LabelStmt rhsLabel = labelGenerator.gen();
                    backpatch(lhsChunk.truejumps, rhsLabel);
                    statements.addAll(lhsChunk.statements);
                    statements.add(rhsLabel);
                    statements.addAll(rhsChunk.statements);

                    truejumps.addAll(rhsChunk.truejumps);
                    falsejumps.addAll(lhsChunk.falsejumps);
                    falsejumps.addAll(rhsChunk.falsejumps);
                    return new CondChunk(statements, truejumps, falsejumps);
                }
                case OR: { // ||
                    CondChunk lhsChunk = doCond(lhs, method);
                    CondChunk rhsChunk = doCond(rhs, method);
                    // Create a label for rhs
                    Ir3.LabelStmt rhsLabel = labelGenerator.gen();
                    backpatch(lhsChunk.falsejumps, rhsLabel);
                    statements.addAll(lhsChunk.statements);
                    statements.add(rhsLabel);
                    statements.addAll(rhsChunk.statements);

                    truejumps.addAll(lhsChunk.truejumps);
                    truejumps.addAll(rhsChunk.truejumps);
                    falsejumps.addAll(rhsChunk.falsejumps);
                    return new CondChunk(statements, truejumps, falsejumps);
                }
                case LT:
                case GT:
                case LEQ:
                case GEQ:
                case EQ:
                case NEQ: {
                    RvalRes lhsChunk = doRval(lhs, method);
                    RvalRes rhsChunk = doRval(rhs, method);
                    statements.addAll(lhsChunk.statements);
                    statements.addAll(rhsChunk.statements);
                    Ir3.CmpStmt cmpStmt = new Ir3.CmpStmt(op, lhsChunk.rv, rhsChunk.rv);
                    statements.add(cmpStmt);
                    truejumps.add(cmpStmt);
                    Ir3.GotoStmt gotoStmt = new Ir3.GotoStmt();
                    statements.add(gotoStmt);
                    falsejumps.add(gotoStmt);
                    return new CondChunk(statements, truejumps, falsejumps);
                }
            }
        } else {
            System.out.println("Unhandled expr type: " + expr.getClass().toString());
        }
        return new CondChunk(statements, truejumps, falsejumps);
    }

    private class TempGenerator {
        int counter;

        TempGenerator() {
            counter = 0;
        }

        Ir3.Var gen(Ast.Typ typ, Ir3.Method method) {
            String tempName = "_t" + counter++;
            Ir3.Var v = new Ir3.Var(typ, tempName);
            method.locals.add(v);
            return v;
        }
    }

    private class LabelGenerator {
        int counter;

        LabelGenerator() {
            counter = 0;
        }

        Ir3.LabelStmt gen() {
            String label = "Label" + counter++;
            return new Ir3.LabelStmt(label);
        }
    }

    private class RvalRes {
        Ir3.Rval rv;
        ArrayList<Ir3.Stmt> statements;

        RvalRes(Ir3.Rval rv, ArrayList<Ir3.Stmt> statements) {
            this.rv = rv;
            this.statements = statements;
        }
    }

    private class StmtChunk {
        ArrayList<Ir3.Stmt> stmts;
        ArrayList<Ir3.JumpStmt> nextjumps;

        StmtChunk(ArrayList<Ir3.Stmt> stmts, ArrayList<Ir3.JumpStmt> nextjumps) {
            this.stmts = stmts;
            this.nextjumps = nextjumps;
        }
    }

    private class CondChunk {
        private final ArrayList<Ir3.Stmt> statements;
        private final ArrayList<Ir3.JumpStmt> truejumps;
        private final ArrayList<Ir3.JumpStmt> falsejumps;

        private CondChunk(ArrayList<Ir3.Stmt> statements, ArrayList<Ir3.JumpStmt> truejumps, ArrayList<Ir3.JumpStmt> falsejumps) {
            this.statements = statements;
            this.truejumps = truejumps;
            this.falsejumps = falsejumps;
        }
    }
}