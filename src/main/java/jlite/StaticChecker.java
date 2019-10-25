package jlite;

import jlite.exceptions.SemanticException;
import jlite.parser.Ast;
import jlite.parser.parser;

import java.util.*;

public class StaticChecker {
    private HashMap<String, ClasDescriptor> classDescs = new HashMap<>();

    public void run(Ast.Prog prog) throws SemanticException {
        this.init(prog); // May throw, if already semantically invalid

        // Proceed with type-checking
        for (Ast.Clas clas : prog.clasList) {
            checkClass(clas);
        }
    }

    private void checkClass(Ast.Clas clas) throws SemanticException {
        ClasDescriptor desc = classDescs.get(clas.cname);
        Env env = new Env();
        env.populate(desc);

        for (Ast.MdDecl mdDecl : clas.mdDeclList) {
            checkMdDecl(mdDecl, env);
        }
    }

    private void checkMdDecl(Ast.MdDecl mdDecl, Env parent) throws SemanticException {
        ArrayList<SemanticException> errors = new ArrayList<>();
        Env env = new Env(parent); // We create a new environment for the method
        env.populate(mdDecl, classDescs);

        Ast.Typ lastTyp = checkStmts(mdDecl.stmts, env);

        if (!lastTyp.isSubTypeOrEquals(mdDecl.retTyp)) {
            throw new SemanticException(mdDecl, String.format("Method body type '%s' not equal to return type '%s'", lastTyp, mdDecl.retTyp));
        }
    }

    private Ast.Typ checkStmts(List<Ast.Stmt> stmts, Env env) throws SemanticException {
        Ast.Typ lastTyp = null;

        for (Ast.Stmt stmt : stmts) {
            lastTyp = checkStmt(stmt, env);
        }

        if (lastTyp == null) {
            throw new SemanticException("Statement body cannot be empty.");
        }

        return lastTyp;
    }

    private Ast.Typ checkStmt(Ast.Stmt stmt, Env env) throws SemanticException {
        if (stmt instanceof Ast.IfStmt) {
            Ast.IfStmt ifStmt = (Ast.IfStmt) stmt;
            Ast.Expr cond = ifStmt.cond;

            Ast.Typ condTyp = checkExpr(cond, env);
            if (!condTyp.equals(new Ast.BoolTyp())) {
                throw new SemanticException(cond, String.format("Cond '%s': returns %s, expecting boolean.", cond.print(), condTyp.toString()));
            }

            List<Ast.Stmt> thenStmtList = ifStmt.thenStmtList;
            List<Ast.Stmt> elseStmtList = ifStmt.elseStmtList;

            Ast.Typ thenStmtTyp = null;
            Ast.Typ elseStmtTyp = null;

            thenStmtTyp = checkStmts(thenStmtList, env);
            elseStmtTyp = checkStmts(elseStmtList, env);

            if (!(thenStmtTyp.isSubTypeOrEquals(elseStmtTyp) || elseStmtTyp.isSubTypeOrEquals(thenStmtTyp))) {
                throw new SemanticException(cond, String.format("then block and else block type incompatible. then: '%s', else '%s'", thenStmtTyp, elseStmtTyp));
            }

            return thenStmtTyp.isSubTypeOrEquals(elseStmtTyp) ? elseStmtTyp : thenStmtTyp; // Return the most general type
        } else if (stmt instanceof Ast.WhileStmt) {
            Ast.WhileStmt whileStmt = (Ast.WhileStmt) stmt;
            Ast.Typ condTyp = checkExpr(whileStmt.cond, env);

            if (!condTyp.isSubTypeOrEquals(new Ast.BoolTyp())) {
                throw new SemanticException(whileStmt.cond, String.format("condition in while statement needs to be bool. Got '%s'", condTyp));
            }

            Ast.Typ whileStmtTyp = checkStmts(whileStmt.stmtList, env);

            return whileStmtTyp;
        } else if (stmt instanceof Ast.ReadlnStmt) {
            Ast.ReadlnStmt readlnStmt = (Ast.ReadlnStmt) stmt;

            if (!env.contains(readlnStmt.ident)) {
                throw new SemanticException(stmt, String.format("Unknown symbol: '%s'", readlnStmt.ident));
            }

            Ast.Typ identTyp = env.getOne(readlnStmt.ident);

            ArrayList<Ast.Typ> validTypes = new ArrayList<>();
            validTypes.add(new Ast.IntTyp());
            validTypes.add(new Ast.StringTyp());
            validTypes.add(new Ast.BoolTyp());
            if (!validTypes.contains(identTyp)) {
                throw new SemanticException(stmt, "ident not of type Int, String or Bool.'");
            }
            // TODO: need to assign vardecl for readln?
            return new Ast.VoidTyp();
        } else if (stmt instanceof Ast.PrintlnStmt) {
            Ast.PrintlnStmt printlnStmt = (Ast.PrintlnStmt) stmt;
            Ast.Typ exprTyp = checkExpr(printlnStmt.expr, env);

            ArrayList<Ast.Typ> validTypes = new ArrayList<>();
            validTypes.add(new Ast.IntTyp());
            validTypes.add(new Ast.StringTyp());
            validTypes.add(new Ast.BoolTyp());

            if (!(validTypes.contains(exprTyp))) {
                throw new SemanticException(stmt, String.format("println statement expr of type '%s', expecting Int, String or Bool", exprTyp));
            }
        } else if (stmt instanceof Ast.VarAssignStmt) {
            Ast.VarAssignStmt varAssignStmt = (Ast.VarAssignStmt) stmt;
            String ident = varAssignStmt.lhs;
            Ast.Expr rhs = varAssignStmt.rhs;

            if (!env.contains(ident))
                throw new SemanticException(varAssignStmt, String.format("unknown symbol '%s'", ident));
            Ast.Typ identTyp = env.getOne(ident);

            Ast.Typ rhsTyp = checkExpr(rhs, env);

            if (!rhsTyp.isSubTypeOrEquals(identTyp)) {
                throw new SemanticException(rhs, String.format("varassign: rhs '%s' not subtype of ident type '%s'.", rhsTyp, identTyp));
            }

            // TODO: need to get vardecl?

            return new Ast.VoidTyp();
        } else if (stmt instanceof Ast.FieldAssignStmt) {
            Ast.FieldAssignStmt fieldAssignStmt = (Ast.FieldAssignStmt) stmt;
            Ast.Typ lhsExprTyp = checkExpr(fieldAssignStmt.lhsExpr, env);
            Ast.Typ rhsExprTyp = checkExpr(fieldAssignStmt.rhs, env);

            if (!(lhsExprTyp instanceof Ast.ClasTyp)) {
                throw new SemanticException(fieldAssignStmt.lhsExpr, String.format("fieldassign: lhs expr of type '%s', expecting Class", lhsExprTyp));
            }

            String cname = ((Ast.ClasTyp) lhsExprTyp).cname;

            if (!classDescs.containsKey(cname)) {
                throw new SemanticException(fieldAssignStmt.lhsExpr, String.format("fieldassign: no class '%s'", cname));
            }

            ClasDescriptor desc = classDescs.get(cname);

            if (!desc.vars.containsKey(fieldAssignStmt.lhsField)) {
                throw new SemanticException(fieldAssignStmt.lhsExpr, String.format("fieldassign: class '%s' does not have field '%s'.", cname, fieldAssignStmt.lhsField));
            }

            if (!rhsExprTyp.isSubTypeOrEquals(lhsExprTyp)) {
                throw new SemanticException(fieldAssignStmt.rhs, String.format("fieldassign: rhs of type '%s' not subtype of lhs of type '%s'", rhsExprTyp, lhsExprTyp));
            }

            return new Ast.VoidTyp();
        } else if (stmt instanceof Ast.ReturnStmt) {
            Ast.ReturnStmt returnStmt = (Ast.ReturnStmt) stmt;
            Ast.Typ retTyp = env.getOne("Ret");
            assert retTyp != null;

            if (retTyp instanceof Ast.VoidTyp) {
                if (returnStmt.expr != null) {
                    throw new SemanticException(stmt, "return: cannot return an expression for Void statement");
                }
                return retTyp;
            } else {
                if (returnStmt.expr == null) {
                    throw new SemanticException(returnStmt.expr, "return: expr not null, but return type is null");
                }

                Ast.Typ exprTyp = checkExpr(returnStmt.expr, env);
                if (!exprTyp.isSubTypeOrEquals(retTyp)) {
                    throw new SemanticException(returnStmt.expr, String.format("return: exprTyp '%s' not subtype of retTyp '%s'", exprTyp, retTyp));
                }

                return retTyp;
            }
        } else if (stmt instanceof Ast.CallStmt) {
            Ast.CallStmt callStmt = (Ast.CallStmt) stmt;
            Ast.CallExpr callExpr = new Ast.CallExpr(callStmt.target, callStmt.args);

            Ast.Typ callTyp = checkExpr(callExpr, env);

            return callTyp;
        } else {
            assert (false); // Should not reach here
        }
        return new Ast.VoidTyp();
    }

    private Ast.Typ checkExpr(Ast.Expr expr, Env env) throws SemanticException {
        if (expr instanceof Ast.IntLitExpr) {
            expr.typ = new Ast.IntTyp();
            return expr.typ;
        } else if (expr instanceof Ast.StringLitExpr) {
            expr.typ = new Ast.StringTyp();
            return expr.typ;
        } else if (expr instanceof Ast.NullLitExpr) {
            expr.typ = new Ast.NullTyp();
            return expr.typ;
        } else if (expr instanceof Ast.BoolLitExpr) {
            expr.typ = new Ast.BoolTyp();
            return expr.typ;
        } else if (expr instanceof Ast.ThisExpr) {
            if (env.contains("this")) {
                expr.typ = env.getOne("this");
            } else {
                throw new SemanticException(expr, "unknown symbol 'this'");
            }
        } else if (expr instanceof Ast.IdentExpr) {
            String ident = ((Ast.IdentExpr) expr).ident;
            if (env.contains(ident)) {
                expr.typ = env.getOne(ident);
            } else {
                throw new SemanticException(expr, String.format("unknown symbol '%s'", ident));
            }
        } else if (expr instanceof Ast.UnaryExpr) {
            Ast.UnaryExpr unaryExpr = (Ast.UnaryExpr) expr;
            Ast.Expr uexpr = unaryExpr.expr;
            Ast.Typ ut = checkExpr(uexpr, env);
            switch (unaryExpr.op) {
                case NOT:
                    if (!ut.isSubTypeOrEquals(new Ast.BoolTyp())) {
                        throw new SemanticException(ut, String.format("NOT operation expects boolean, got %%s%s", ut));
                    }
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                case NEGATIVE:
                    if (!ut.isSubTypeOrEquals(new Ast.IntTyp())) {
                        throw new SemanticException(ut, String.format("NEG operation expects int, got %%s%s", ut));
                    }
                    expr.typ = new Ast.IntTyp();
                    return expr.typ;
            }
        } else if (expr instanceof Ast.BinaryExpr) {
            Ast.BinaryExpr binaryExpr = (Ast.BinaryExpr) expr;
            Ast.BinaryOp op = binaryExpr.op;
            Ast.Expr lhs = binaryExpr.lhs;
            Ast.Expr rhs = binaryExpr.rhs;
            Ast.Typ lhsTyp = checkExpr(lhs, env);
            Ast.Typ rhsTyp = checkExpr(rhs, env);

            switch (op) {
                case PLUS:
                case MINUS:
                case MULT:
                case DIV:
                    // [Arith]
                    if (!lhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(lhs, String.format("%s: expected assignable to Int on lhs, got: %s", op, lhsTyp));
                    if (!rhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(rhs, String.format("%s: expected assignable to Int on rhs, got: %s", op, rhsTyp));
                    expr.typ = new Ast.IntTyp();
                    return expr.typ;
                case LT:
                case GT:
                case LEQ:
                case GEQ:
                    // [Rel]
                    if (!lhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(lhs, String.format("%s: expected assignable to Int on lhs, got: %s", op, lhsTyp));
                    if (!rhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(rhs, String.format("%s: expected assignable to Int on rhs, got: %s", op, rhsTyp));
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                case EQ:
                case NEQ:
                    // [Rel] enhanced for subtyping
                    // lhs is assignable to rhs or rhs is assignable to lhs
                    if (!(lhsTyp.isSubTypeOrEquals(rhsTyp) || rhsTyp.isSubTypeOrEquals(lhsTyp)))
                        throw new SemanticException(expr, String.format("%s: expected LHS/RHS types to be compatible, got lhs: %s and rhs: %s", op, lhsTyp, rhsTyp));
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                case AND:
                case OR:
                    // [Bool]
                    if (!lhsTyp.isSubTypeOrEquals(new Ast.BoolTyp()))
                        throw new SemanticException(expr, String.format("%s: expected assignable to Bool on lhs, got: %s", op, lhsTyp));
                    if (!rhsTyp.isSubTypeOrEquals(new Ast.BoolTyp()))
                        throw new SemanticException(expr, String.format("%s: expected assignable to Bool on rhs, got: %s", op, rhsTyp));
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                default:
                    throw new AssertionError("Jlite Compiler Error: should not reach here!");
            }
        } else if (expr instanceof Ast.DotExpr) {
            Ast.DotExpr dotExpr = (Ast.DotExpr) expr;
            Ast.Typ targetTyp = checkExpr(dotExpr.target, env); // can throw
            if (!(targetTyp instanceof Ast.ClasTyp)) {
                throw new SemanticException(expr, String.format("dotexpr: expected class type, got '%s'", targetTyp));
            }

            Ast.ClasTyp targetClasTyp = (Ast.ClasTyp) targetTyp;

            if (!classDescs.containsKey(targetClasTyp.cname)) {
                throw new SemanticException(dotExpr.target, String.format("dotexpr: no such class '%s'", targetClasTyp.cname));
            }

            ClasDescriptor desc = classDescs.get(targetClasTyp.cname);

            if (!desc.vars.containsKey(dotExpr.ident)) {
                throw new SemanticException(dotExpr.target, String.format("dotexpr: no such field '%s' in class '%s'", dotExpr.ident, targetClasTyp.cname));
            }

            expr.typ = desc.vars.get(dotExpr.ident).type;
            return expr.typ;

        } else if (expr instanceof Ast.CallExpr) {
            Ast.CallExpr callExpr = (Ast.CallExpr) expr;
            if (callExpr.target instanceof Ast.IdentExpr) {
                Ast.IdentExpr identExpr = (Ast.IdentExpr) callExpr.target;

                if (!env.contains(identExpr.ident)) {
                    throw new SemanticException(identExpr, String.format("callexpr: no symbol '%s'", identExpr.ident));
                }

                ArrayList<Ast.Typ> argTyps = new ArrayList<>();

                for (Ast.Expr arg : callExpr.args) {
                    argTyps.add(checkExpr(arg, env));
                }

                Collection<Ast.Typ> candidates = env.get(identExpr.ident);
                if (candidates == null) {
                    throw new SemanticException(identExpr, String.format("No such target in env:'%s'", identExpr.ident));
                }

                boolean hasCandidate = false;
                Ast.Typ retTyp = null;
                for (Ast.Typ candidate : candidates) {
                    if (candidate instanceof Ast.FuncTyp) {
                        if (((Ast.FuncTyp) candidate).argTyps.equals(argTyps)) {
                            hasCandidate = true;
                            retTyp = ((Ast.FuncTyp) candidate).retTyp;
                        }
                    }
                }

                if (!hasCandidate) {
                    throw new SemanticException(String.format("No method signature '%s' for target '%s'", argTyps.toString(), identExpr.ident));
                }

                assert retTyp != null;
                expr.typ = retTyp;
                return expr.typ;
            } else if (callExpr.target instanceof Ast.DotExpr) {
                Ast.DotExpr dotExpr = (Ast.DotExpr) callExpr.target;
                Ast.Typ targetTyp = checkExpr(dotExpr.target, env);
                if (!(targetTyp instanceof Ast.ClasTyp)) {
                    throw new SemanticException(callExpr.target, String.format("callexpr->dotexpr: target of type '%s', expecting Class", dotExpr.target));
                }
                Ast.ClasTyp targetClasTyp = (Ast.ClasTyp) targetTyp;

                if (!classDescs.containsKey(targetClasTyp.cname)) {
                    throw new SemanticException(callExpr.target, String.format("callexpr: no such class '%s'", targetClasTyp.cname));
                }

                ClasDescriptor desc = classDescs.get(targetClasTyp.cname);

                // Method access
                if (!desc.methods.containsKey(dotExpr.ident)) {
                    throw new SemanticException(dotExpr, String.format("callexpr: class '%s' does not have method '%s'", targetClasTyp.cname, dotExpr.ident));
                }

                ArrayList<Ast.Typ> argTyps = new ArrayList<>();

                for (Ast.Expr arg : callExpr.args) {
                    argTyps.add(checkExpr(arg, env));
                }

                Set<Ast.FuncTyp> candidates = desc.methods.get(dotExpr.ident);
                if (candidates.isEmpty()) {
                    throw new SemanticException(dotExpr, String.format("No such method '%s' in class '%s'", dotExpr.ident, targetClasTyp.cname));
                }

                boolean hasCandidate = false;
                Ast.Typ retTyp = null;
                for (Ast.Typ candidate : candidates) {
                    if (candidate instanceof Ast.FuncTyp) {
                        if (((Ast.FuncTyp) candidate).argTyps.equals(argTyps)) {
                            hasCandidate = true;
                            retTyp = ((Ast.FuncTyp) candidate).retTyp;
                        }
                    }
                }

                if (!hasCandidate) {
                    throw new SemanticException(dotExpr, String.format("No method signature '%s' for target '%s'", argTyps.toString(), dotExpr.ident));
                }

                assert retTyp != null;
                expr.typ = retTyp;
                return expr.typ;

            } else {
                throw new SemanticException(expr, String.format("callexpr: expecting ident or dotexpr, got '%s'", callExpr.getClass().toString()));
            }
        } else if (expr instanceof Ast.NewExpr) {
            Ast.NewExpr newExpr = (Ast.NewExpr) expr;
            if (!classDescs.containsKey(newExpr.cname))
                throw new SemanticException(String.format("newexpr: no such class '%s'", newExpr.cname));
            expr.typ = new Ast.ClasTyp(newExpr.cname);
            return expr.typ;
        } else {
            throw new SemanticException(expr, String.format("Unhandled expr type: %s", expr.getClass().toString()));
        }

        return expr.typ;
    }

    /**
     * Augments the parse tree to form a syntax tree with type details.
     * <p>
     * While populating, check the following:
     * 1. There are no 2 classes with the same name
     * 2. For each class, no 2 fields have the same name
     * 3. The types of every varDecl is valid
     * 4. Methods with the same signature cannot have the same name
     *
     * @param prog AST generated by the parser
     * @throws SemanticException First Semantic Error detected by checker
     */
    private void init(Ast.Prog prog) throws SemanticException {
        for (Ast.Clas c : prog.clasList) {
            // Check if class name already exists
            if (classDescs.containsKey(c.cname)) {
                throw new SemanticException(c, String.format("Duplicate class name '%s'", c.cname));
            }

            ClasDescriptor desc = new ClasDescriptor(c);
            classDescs.put(c.cname, desc);
        }

        for (Ast.Clas c : prog.clasList) {
            ClasDescriptor desc = classDescs.get(c.cname);
            for (Ast.VarDecl varDecl : c.varDeclList) {
                // Check if the declaration types are valid
                if (!isValidType(varDecl.type)) {
                    throw new SemanticException(varDecl, String.format("Invalid var type '%s'", varDecl.type));
                }

                // Check if there are duplicate var declarations
                if (desc.vars.containsKey(varDecl.ident)) {
                    throw new SemanticException(varDecl, String.format("Duplicate var declaration '%s'", varDecl.ident));
                } else {
                    desc.vars.put(varDecl.ident, varDecl);
                }
            }

            for (Ast.MdDecl mdDecl : c.mdDeclList) {
                HashMap<String, Ast.VarDecl> args = new HashMap<>();

                for (Ast.VarDecl arg : mdDecl.args) {
                    if (args.containsKey(arg.ident)) {
                        throw new SemanticException(arg, String.format("Duplicate arg name '%s' in method '%s' of class '%s'", arg.ident, mdDecl.name, c.cname));
                    }

                    if (!isValidType(arg.type)) {
                        throw new SemanticException(arg, String.format("Invalid argument type '%s' for arg '%s' in method '%s' of class '%s'", arg.type, arg.ident, mdDecl.name, c.cname));
                    }

                    args.put(arg.ident, arg);
                }

                Ast.FuncTyp funcTyp = new Ast.FuncTyp(mdDecl);
                if (desc.hasMethodSignature(mdDecl.name, funcTyp)) {
                    throw new SemanticException(mdDecl, String.format("Duplicate method signature '%s' for method '%s' of class '%s'", funcTyp.toString(), mdDecl.name, c.cname));
                } else {
                    desc.addMethodSignature(mdDecl.name, funcTyp);
                }
            }
        }
    }

    /**
     * @param type
     * @return True if a base type (String etc.) or it is a class type and the program has a class with that name
     */
    private boolean isValidType(Ast.Typ type) {
        return !(type instanceof Ast.ClasTyp) || classDescs.containsKey(((Ast.ClasTyp) type).cname);
    }

    public static void main(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String fileLoc = argv[i];
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                System.out.println(prog.toJSON());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
