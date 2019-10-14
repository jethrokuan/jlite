package jlite;

import jlite.exceptions.SemanticErrors;
import jlite.exceptions.SemanticException;
import jlite.parser.Ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class StaticChecker {
    private HashMap<String, ClasDescriptor> classDescs = new HashMap<>();

    public void run(Ast.Prog prog) throws SemanticErrors {
        this.init(prog); // May throw, if already semantically invalid

        ArrayList<SemanticException> errors = new ArrayList<>();

        // Proceed with type-checking
        for (Ast.Clas clas : prog.clasList) {
            errors.addAll(checkClass(clas));
        }

        if (!errors.isEmpty()) throw new SemanticErrors(errors);
    }

    private ArrayList<SemanticException> checkClass(Ast.Clas clas) {
        ArrayList<SemanticException> errors = new ArrayList<>();
        Env env = new Env(classDescs);
        env.populate(clas.desc);

        for (Ast.MdDecl mdDecl : clas.mdDeclList) {
            errors.addAll(checkMdDecl(mdDecl, env));
        }

        return errors;
    }

    private ArrayList<SemanticException> checkMdDecl(Ast.MdDecl mdDecl, Env parent) {
        ArrayList<SemanticException> errors = new ArrayList<>();
        Env env = new Env(parent, classDescs); // We create a new environment for the method
        errors.addAll(env.populate(mdDecl));
        if (!errors.isEmpty()) {
            return errors;
        }

        try {
            Ast.Typ lastTyp = checkStmts(mdDecl.stmts, env);
        } catch (SemanticErrors semanticErrors) {
            errors.addAll(semanticErrors.getErrors());
        }

        return errors;
    }

    private Ast.Typ checkStmts(List<Ast.Stmt> stmts, Env env) throws SemanticErrors {
        ArrayList<SemanticException> errors = new ArrayList<>();

        Ast.Typ lastTyp = null;
        for (Ast.Stmt stmt : stmts) {
            try {
                lastTyp = checkStmt(stmt, env);
            } catch (SemanticErrors semanticErrors) {
                errors.addAll(semanticErrors.getErrors());
            }
        }

        if (lastTyp == null) {
            errors.add(new SemanticException("Statement body cannot be empty."));
        }

        if (!errors.isEmpty()) {
            throw new SemanticErrors(errors);
        }

        return lastTyp;
    }

    private Ast.Typ checkStmt(Ast.Stmt stmt, Env env) throws SemanticErrors {
        ArrayList<SemanticException> errors = new ArrayList<>();

        if (stmt instanceof Ast.IfStmt) {
            Ast.IfStmt ifStmt = (Ast.IfStmt) stmt;
            Ast.Expr cond = ifStmt.cond;

            try {
                Ast.Typ condTyp = checkExpr(cond, env);
                if (!condTyp.equals(new Ast.BoolTyp())) {
                    errors.add(new SemanticException(String.format("Cond '%s': returns %s, expecting boolean.", cond.print(), condTyp.toString())));
                }
            } catch (SemanticException e) {
                errors.add(e);
            }

            List<Ast.Stmt> thenStmtList = ifStmt.thenStmtList;
            List<Ast.Stmt> elseStmtList = ifStmt.elseStmtList;

            Ast.Typ thenStmtTyp = null;
            Ast.Typ elseStmtTyp = null;

            try {
                thenStmtTyp = checkStmts(thenStmtList, env);
            } catch (SemanticErrors e) {
                errors.addAll(e.getErrors());
            }

            try {
                elseStmtTyp = checkStmts(elseStmtList, env);
            } catch (SemanticErrors e) {
                errors.addAll(e.getErrors());
            }

            if (!errors.isEmpty()) {
                throw new SemanticErrors(errors);
            }

            if (!(thenStmtTyp.equals(elseStmtTyp) || elseStmtTyp.equals(thenStmtTyp))) {
                errors.add(new SemanticException(String.format("then block and else block type incompatible. then: '%s', else '%s'", thenStmtTyp, elseStmtTyp)));
            }

            if (!errors.isEmpty()) {
                throw new SemanticErrors(errors);
            }

            return thenStmtTyp.equals(elseStmtTyp) ? elseStmtTyp : thenStmtTyp;
        } else if (stmt instanceof Ast.WhileStmt) {
            // TODO
        }
        if (!errors.isEmpty()) {
            throw new SemanticErrors(errors);
        }

        return new Ast.VoidTyp();
    }

    private Ast.Typ checkExpr(Ast.Expr expr, Env env) throws SemanticException {
        if (expr instanceof Ast.IntLitExpr) {
            expr.typ = new Ast.IntTyp();
            return new Ast.IntTyp();
        } else if (expr instanceof Ast.StringLitExpr) {
            expr.typ = new Ast.StringTyp();
        } else if (expr instanceof Ast.NullLitExpr) {
            expr.typ = new Ast.NullTyp();
        } else if (expr instanceof Ast.BoolLitExpr) {
            expr.typ = new Ast.BoolTyp();
        } else if (expr instanceof Ast.ThisExpr) {
            if (env.contains("this")) {
                expr.typ = env.get("this");
            } else {
                throw new SemanticException("unknown symbol 'this'");
            }
        } else if (expr instanceof Ast.IdentExpr) {
            String ident = ((Ast.IdentExpr) expr).ident;
            if (env.contains(ident)) {
                expr.typ = env.get(ident);
            } else {
                throw new SemanticException(String.format("unknown symbol '%s'", ident));
            }
        } else if (expr instanceof Ast.DotExpr) {
            Ast.DotExpr dotExpr = (Ast.DotExpr) expr;
            Ast.Typ targetTyp = checkExpr(dotExpr.target, env);
            String ident = dotExpr.ident;

            if (!(targetTyp instanceof Ast.ClasTyp)) {
                throw new SemanticException(String.format("Field access operation expects class, got '%s'", targetTyp));
            }

            Ast.ClasTyp clasTyp = (Ast.ClasTyp) targetTyp;

            if (!classDescs.containsKey(clasTyp.cname)) {
                throw new SemanticException(String.format("No such class: '%s'", clasTyp.cname));
            }

            ClasDescriptor desc = classDescs.get(clasTyp.cname);

            if (!desc.vars.containsKey(ident)) {
                throw new SemanticException(String.format("Class '%s' has no such field '%s'", clasTyp.cname, ident));
            } else {
                expr.typ = desc.vars.get(clasTyp.cname).type;
            }
            return expr.typ;
        } else if (expr instanceof Ast.UnaryExpr) {
            Ast.UnaryExpr unaryExpr = (Ast.UnaryExpr) expr;
            Ast.Expr uexpr = unaryExpr.expr;
            Ast.Typ ut = checkExpr(uexpr, env);
            switch (unaryExpr.op) {
                case NOT:
                    if (!ut.isSubTypeOrEquals(new Ast.BoolTyp())) {
                        throw new SemanticException(String.format("NOT operation expects boolean, got %%s%s", ut));
                    }
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                case NEGATIVE:
                    if (!ut.isSubTypeOrEquals(new Ast.IntTyp())) {
                        throw new SemanticException(String.format("NEG operation expects int, got %%s%s", ut));
                    }
                    expr.typ = new Ast.IntTyp();
                    return expr.typ;
            }
        } else if (expr instanceof Ast.BinaryExpr) {
            Ast.BinaryOp op = ((Ast.BinaryExpr) expr).op;
            Ast.Expr lhs = ((Ast.BinaryExpr) expr).lhs;
            Ast.Expr rhs = ((Ast.BinaryExpr) expr).rhs;
            Ast.Typ lhsTyp = checkExpr(lhs, env);
            Ast.Typ rhsTyp = checkExpr(rhs, env);

            switch (op) {
                case PLUS:
                case MINUS:
                case MULT:
                case DIV:
                    // [Arith]
                    if (!lhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(expr.toString() + "" + op + ": expected assignable to Int on lhs, got: " + lhsTyp);
                    if (!rhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(expr.toString() + "" + op + ": expected assignable to Int on rhs, got: " + rhsTyp);
                    expr.typ = new Ast.IntTyp();
                case LT:
                case GT:
                case LEQ:
                case GEQ:
                    // [Rel]
                    if (!lhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(expr.toString() + "" + op + ": expected assignable to Int on lhs, got: " + lhsTyp);
                    if (!rhsTyp.isSubTypeOrEquals(new Ast.IntTyp()))
                        throw new SemanticException(expr.toString() + "" + op + ": expected assignable to Int on rhs, got: " + rhsTyp);
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                case EQ:
                case NEQ:
                    // [Rel] enhanced for subtyping
                    // lhs is assignable to rhs or rhs is assignable to lhs
                    if (!(lhsTyp.isSubTypeOrEquals(rhsTyp) || rhsTyp.isSubTypeOrEquals(lhsTyp)))
                        throw new SemanticException(expr + "" + op + ": expected LHS/RHS types to be compatible, got lhs: " + lhsTyp + " and rhs: " + rhsTyp);
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                case AND:
                case OR:
                    // [Bool]
                    if (!lhsTyp.isSubTypeOrEquals(new Ast.BoolTyp()))
                        throw new SemanticException(expr + "" + op + ": expected assignable to Bool on lhs, got: " + lhsTyp);
                    if (!rhsTyp.isSubTypeOrEquals(new Ast.BoolTyp()))
                        throw new SemanticException(expr + "" + op + ": expected assignable to Bool on rhs, got: " + rhsTyp);
                    expr.typ = new Ast.BoolTyp();
                    return expr.typ;
                default:
                    throw new AssertionError("BUG");
            }
        } else {
            throw new SemanticException(String.format("Unhandled expr type: %s", expr.getClass().toString()));
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
     * @throws SemanticErrors
     */
    private void init(Ast.Prog prog) throws SemanticErrors {
        ArrayList<SemanticException> errors = new ArrayList<>();

        for (Ast.Clas c : prog.clasList) {
            // Check if class name already exists
            if (classDescs.containsKey(c.cname)) {
                errors.add(new SemanticException(String.format("Duplicate class name '%s'", c.cname)));
            }

            ClasDescriptor desc = new ClasDescriptor(c);
            c.desc = desc;
            classDescs.put(c.cname, desc);
        }

        if (!errors.isEmpty()) {
            throw new SemanticErrors(errors);
        }

        for (Ast.Clas c : prog.clasList) {
            for (Ast.VarDecl varDecl : c.varDeclList) {
                // Check if the declaration types are valid
                if (!isValidType(varDecl.type)) {
                    errors.add(new SemanticException(String.format("Invalid var type '%s'", varDecl.type)));
                }

                // Check if there are duplicate var declarations
                if (c.desc.vars.containsKey(varDecl.ident)) {
                    errors.add(new SemanticException(String.format("Duplicate var declaration '%s'", varDecl.ident)));
                } else {
                    c.desc.vars.put(varDecl.ident, varDecl);
                }
            }

            for (Ast.MdDecl mdDecl : c.mdDeclList) {
                HashMap<String, Ast.VarDecl> args = new HashMap<>();

                for (Ast.VarDecl arg : mdDecl.args) {
                    if (args.containsKey(arg.ident)) {
                        errors.add(new SemanticException(String.format("Duplicate arg name '%s' in method '%s' of class '%s'", arg.ident, mdDecl.name, c.cname)));
                    }

                    if (!isValidType(arg.type)) {
                        errors.add(new SemanticException(String.format("Invalid argument type '%s' for arg '%s' in method '%s' of class '%s'", arg.type, arg.ident, mdDecl.name, c.cname)));
                    }

                    args.put(arg.ident, arg);
                }

                Ast.FuncTyp funcTyp = new Ast.FuncTyp(mdDecl);
                if (c.desc.hasMethodSignature(mdDecl.name, funcTyp)) {
                    errors.add(new SemanticException(String.format("Duplicate method signature '%s' for method '%s' of class '%s'", funcTyp.toString(), mdDecl.name, c.cname)));
                } else {
                    c.desc.addMethodSignature(mdDecl.name, funcTyp);
                }
            }
        }

        if (!errors.isEmpty()) throw new SemanticErrors(errors);
    }

    /**
     * @param type
     * @return True if a base type (String etc.) or it is a class type and the program has a class with that name
     */
    private boolean isValidType(Ast.Typ type) {
        if (type instanceof Ast.ClasTyp) return classDescs.containsKey(((Ast.ClasTyp) type).cname);
        return true;
    }
}
