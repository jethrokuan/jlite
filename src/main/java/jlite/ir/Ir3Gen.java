package jlite.ir;

import jlite.StaticChecker;
import jlite.parser.Ast;
import jlite.parser.parser;

import java.util.ArrayList;
import java.util.HashMap;

public class Ir3Gen {

    TempGenerator tempGenerator = new TempGenerator();
    LabelGenerator labelGenerator = new LabelGenerator();
    private ArrayList<Ir3.Data> datas = new ArrayList<>();
    private HashMap<String, Ir3.Data> dataMap = new HashMap<>();
    private ArrayList<Ir3.Method> methods = new ArrayList<>();
    private HashMap<Ast.MdDecl, Ir3.Method> methodMap = new HashMap<>();

    public static void main(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String fileLoc = argv[i];
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
        }
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
                if (mdDecl.name == "main") {
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

            ArrayList<Ir3.Stmt> irStmts = new ArrayList<>();

            for (Ast.Stmt stmt : mdDecl.stmts) {
                irStmts.addAll(genStmt(stmt, method));
            }

            method.args.addAll(args);
            method.locals.addAll(locals);
            method.statements.addAll(irStmts);
        }
    }

    private ArrayList<Ir3.Stmt> genStmt(Ast.Stmt stmt, Ir3.Method method) {
        ArrayList<Ir3.Stmt> statementList = new ArrayList<>();
        if (stmt instanceof Ast.ReadlnStmt) {
            Ast.ReadlnStmt readlnStmt = (Ast.ReadlnStmt) stmt;
            statementList.add(new Ir3.ReadlnStmt(readlnStmt.ident));
            return statementList;
        } else if (stmt instanceof Ast.PrintlnStmt) {
            Ast.PrintlnStmt printlnStmt = (Ast.PrintlnStmt) stmt;
            RvalRes res = doRval(printlnStmt.expr, method);
            statementList.addAll(res.statements);
            statementList.add(new Ir3.PrintlnStmt((Ir3.Var) res.rv));

            return statementList;
        } else if (stmt instanceof Ast.VarAssignStmt) {
            Ast.VarAssignStmt varAssignStmt = (Ast.VarAssignStmt) stmt;
            RvalRes res = doRval(varAssignStmt.rhs, method);
            statementList.addAll(res.statements);
            Ir3.Var var = new Ir3.Var(res.rv.getTyp(), varAssignStmt.lhs); // Hack: we don't care about type
            statementList.add(new Ir3.AssignStmt(var, res.rv));
            return statementList;
        } else if (stmt instanceof Ast.ReturnStmt) {
            Ast.ReturnStmt returnStmt = (Ast.ReturnStmt) stmt;
            Ir3.Rval rv = null;
            if (returnStmt.expr != null) {
                RvalRes res = doRval(returnStmt.expr, method);
                rv = res.rv;
                statementList.addAll(res.statements);
            }
            statementList.add(new Ir3.ReturnStmt(rv));

            return statementList;
        } else if (stmt instanceof Ast.FieldAssignStmt) {

        } else if (stmt instanceof Ast.CallStmt) {
            Ast.CallStmt callStmt = (Ast.CallStmt) stmt;

        } else if (stmt instanceof Ast.IfStmt) {
            Ast.IfStmt ifStmt = (Ast.IfStmt) stmt;
            RvalRes res = doRval(ifStmt.cond, method);
        } else {
            System.out.println("Unhandled stmt type:" + stmt.getClass().toString());
        }
        return statementList;
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
                    // TODO
                }
                case PLUS:
                case MINUS:
                case DIV:
                case MULT: {
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
        } else {
            System.out.println("Unhandled expr type: " + expr.getClass().toString());
        }
        return new RvalRes(null, new ArrayList<>());
    }

    private class TempGenerator {
        int counter;

        public TempGenerator() {
            counter = 0;
        }

        Ir3.Var gen(Ast.Typ typ, Ir3.Method method) {
            String tempName = "_t" + counter;
            Ir3.Var v = new Ir3.Var(typ, tempName);
            method.locals.add(v);
            counter++;
            return v;
        }
    }

    private class LabelGenerator {
        int counter;

        public LabelGenerator() {
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

        public RvalRes(Ir3.Rval rv, ArrayList<Ir3.Stmt> statements) {
            this.rv = rv;
            this.statements = statements;
        }
    }
}
