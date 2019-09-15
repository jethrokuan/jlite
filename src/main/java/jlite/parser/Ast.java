package jlite.parser;

import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Ast {
    public interface GsonPrintable {
        default String toJSON() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(this);
        }
    }

    public static class Prog implements GsonPrintable {
        public final List<Clas> clasList;

        public Prog(List<Clas> clasList) {
            this.clasList = Collections.unmodifiableList(new ArrayList<>(clasList));
        }
    }

    public static class Clas {
        public final String cname;
        public final List<VarDecl> varDeclList;
        public final List<MdDecl> mdDeclList;

        public Clas(String cname, List<VarDecl> varDeclList, List<MdDecl> mdDeclList) {
            this.cname = cname;
            this.varDeclList = Collections.unmodifiableList(new ArrayList<>(varDeclList));
            this.mdDeclList = Collections.unmodifiableList(new ArrayList<>(mdDeclList));
        }
    }

    public static class VarDecl {
        public final Typ type;
        public final String ident;

        public VarDecl(Typ type, String ident) {
            this.type = type;
            this.ident = ident;
        }
    }

    public static class Typ {
        public final String cname;
        public final JliteTyp typ;

        public Typ(JliteTyp typ) {
            this.typ = typ;
            this.cname = null;
        }

        public Typ(JliteTyp typ, String cname) {
            assert(typ == JliteTyp.CLASS);
            this.typ = typ;
            this.cname = cname;
        }
    }

    public static enum JliteTyp {
        INTEGER,
        BOOLEAN,
        STRING,
        VOID,
        CLASS
    }

    public static enum StmtTyp {
        STMT_IF,
        STMT_WHILE,
        STMT_READLN,
        STMT_PRINTLN,
        STMT_VARASSIGN,
        STMT_FIELDASSIGN,
        STMT_RETURN,
        STMT_CALL
    }

    public static abstract class Stmt {
         StmtTyp typ;
    };

    public static class IfStmt extends Stmt {
        public final Expr cond;
        public final List<Stmt> thenStmtList;
        public final List<Stmt> elseStmtList;

        public IfStmt(Expr cond, List<Stmt> thenStmtList, List<Stmt> elseStmtList) {
            this.typ = StmtTyp.STMT_IF;
            this.cond = cond;
            this.thenStmtList = Collections.unmodifiableList(new ArrayList<>(thenStmtList));
            this.elseStmtList = Collections.unmodifiableList(new ArrayList<>(elseStmtList));
        }
    }

    public static class WhileStmt extends Stmt {
        public final Expr cond;
        public final List<Stmt> stmtList;

        public WhileStmt(Expr cond, List<Stmt> stmtList) {
            this.typ = StmtTyp.STMT_WHILE;
            this.cond = cond;
            this.stmtList = Collections.unmodifiableList(new ArrayList<>(stmtList));
        }
    }

    public static class ReadlnStmt extends Stmt {
        public final String ident;

        public ReadlnStmt(String ident) {
            this.typ = StmtTyp.STMT_READLN;
            this.ident = ident;
        }
    }

    public static class PrintlnStmt extends Stmt {
        public final Expr expr;

        public PrintlnStmt(Expr expr) {
            this.typ = StmtTyp.STMT_PRINTLN;
            this.expr = expr;
        }
    }

    public static class VarAssignStmt extends Stmt {
        public final String lhs;
        public final Expr rhs;

        public VarAssignStmt(String lhs, Expr rhs) {
            this.typ = StmtTyp.STMT_VARASSIGN;
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class FieldAssignStmt extends Stmt {
        public final Expr lhsExpr;
        public final String lhsField;
        public final Expr rhs;

        public FieldAssignStmt(Expr lhsExpr, String lhsField, Expr rhs) {
            this.typ = StmtTyp.STMT_FIELDASSIGN;
            this.lhsExpr = lhsExpr;
            this.lhsField = lhsField;
            this.rhs = rhs;
        }
    }

    public static class ReturnStmt extends Stmt {
        public final Expr expr;

        public ReturnStmt(Expr expr) {
            this.typ = StmtTyp.STMT_RETURN;
            this.expr = expr;
        }
    }

    public static class CallStmt extends Stmt {
        public final Expr target;
        public final List<Expr> args;

        public CallStmt(Expr target, List<Expr> args) {
            this.target = target;
            this.args = Collections.unmodifiableList(new ArrayList<>(args));
        }
    }

    public static enum ExprTyp {
        EXPR_STRINGLIT,
        EXPR_INTLIT,
        EXPR_BOOLLIT,
        EXPR_NULLLIT,
        EXPR_THIS,
        EXPR_IDENT,
        EXPR_UNARY,
        EXPR_BINARY,
        EXPR_DOT,
        EXPR_CALL,
        EXPR_NEW
    }

    public static abstract class Expr {
        ExprTyp typ;
    };

    public static class StringLitExpr extends Expr {
        public final String str;

        public StringLitExpr(String str) {
            this.typ = ExprTyp.EXPR_STRINGLIT;
            this.str = str;
        }
    }

    public static class IntLitExpr extends Expr {
        public final int val;

        public IntLitExpr(int val) {
            this.typ = ExprTyp.EXPR_STRINGLIT;
            this.val = val;
        }
    }

    public static class BoolLitExpr extends Expr {
        public final boolean val;

        public BoolLitExpr(boolean val) {
            this.typ = ExprTyp.EXPR_BOOLLIT;
            this.val = val;
        }
    }

    public static class NullLitExpr extends Expr {
        public NullLitExpr() {
            this.typ = ExprTyp.EXPR_NULLLIT;
        }
    }

    public static class ThisExpr extends Expr {
        public ThisExpr() {
            this.typ = ExprTyp.EXPR_THIS;
        }
    }

    public static class IdentExpr extends Expr {
        public final String ident;

        public IdentExpr(String ident) {
            this.typ = ExprTyp.EXPR_IDENT;
            this.ident = ident;
        }
    }

    public static class UnaryExpr extends Expr {
        public final UnaryOp op;
        public final Expr expr;

        public UnaryExpr(UnaryOp op, Expr expr) {
            this.typ = ExprTyp.EXPR_UNARY;
            this.op = op;
            this.expr = expr;
        }
    }

    public static class BinaryExpr extends Expr {
        public final BinaryOp op;
        public final Expr lhs;
        public final Expr rhs;

        public BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) {
            this.typ = ExprTyp.EXPR_BINARY;
            this.op = op;
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class DotExpr extends Expr {
        public final Expr target;
        public final String ident;

        public DotExpr(Expr target, String ident) {
            this.typ = ExprTyp.EXPR_DOT;
            this.target = target;
            this.ident = ident;
        }
    }

    public static class CallExpr extends Expr {
        public final Expr target;
        public final List<Expr> args;

        public CallExpr(Expr target, List<Expr> args) {
            this.typ = ExprTyp.EXPR_CALL;
            this.target = target;
            this.args = args;
        }
    }

public static class NewExpr extends Expr {
        public final String cname;

        public NewExpr(String cname) {
            this.typ = ExprTyp.EXPR_NEW;
            this.cname = cname;
        }
    }

    public static enum UnaryOp {
        NEGATIVE,
        NOT
    }

    public static enum BinaryOp {
        PLUS,
        MINUS,
        MULT,
        DIV,
        LT,
        GT,
        GEQ,
        EQ,
        LEQ,
        NEQ,
        AND,
        OR
    }

    public static class MdDecl {
        public final Typ retTyp;
        public final String name;
        public final List<VarDecl> args;
        public final List<VarDecl> vars;
        public final List<Stmt> stmts;

        public MdDecl(Typ retTyp, String name, List<VarDecl> args, List<VarDecl> vars, List<Stmt> stmts) {
            this.retTyp = retTyp;
            this.name = name;
            this.args = Collections.unmodifiableList(new ArrayList<>(args));
            this.vars = Collections.unmodifiableList(new ArrayList<>(vars));
            this.stmts = Collections.unmodifiableList(new ArrayList<>(stmts));
        }
    }
}
