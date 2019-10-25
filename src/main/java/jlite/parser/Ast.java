package jlite.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class Ast {
    public static void indent(StringBuilder sb, int i) {
        while (i > 0) {
            sb.append("  ");
            i--;
        }
    }

    public interface GsonPrintable {
        default String toJSON() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(this);
        }
    }

    public interface Printable {
        default String print() {
            return this.print(0);
        }

        String print(int indent);
    }

    public static class Location {
        public final int line;
        public final int col;

        public Location(int line, int col) {
            this.line = line;
            this.col = col;
        }
    }

    public static abstract class Locatable {
        public Location location;
    }

    public static class Prog extends Locatable implements GsonPrintable, Printable {
        public final List<Clas> clasList;

        public Prog(List<Clas> clasList) {
            this.clasList = Collections.unmodifiableList(new ArrayList<>(clasList));
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            for (Clas clas : clasList) {
                sb.append(clas.print(i)).append("\n");
            }
            return sb.toString();
        }
    }

    public static class Clas extends Locatable implements Printable {
        public final String cname;
        public final List<VarDecl> varDeclList;
        public final List<MdDecl> mdDeclList;

        public Clas(String cname, List<VarDecl> varDeclList, List<MdDecl> mdDeclList) {
            this.cname = cname;
            this.varDeclList = Collections.unmodifiableList(new ArrayList<>(varDeclList));
            this.mdDeclList = Collections.unmodifiableList(new ArrayList<>(mdDeclList));
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("class ")
                    .append(cname)
                    .append(" {\n");

            i++;

            // Var Declarations
            for (VarDecl v : varDeclList) {
                sb.append(v.print(i))
                        .append(";\n");
            }

            sb.append("\n");

            // Method Declarations

            for (MdDecl m : mdDeclList) {
                sb.append(m.print(i)).append("\n");
            }

            sb.append("}\n");

            return sb.toString();
        }
    }

    public static class VarDecl extends Locatable implements Printable {
        public final Typ type;
        public final String ident;

        public VarDecl(Typ type, String ident) {
            this.type = type;
            this.ident = ident;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(this.type.toString())
                    .append(" ")
                    .append(this.ident);
            return sb.toString();
        }
    }

    public static abstract class Typ extends Locatable {
        String typ;
        public abstract boolean isSubTypeOrEquals(Typ o);
    }

    public static class BoolTyp extends Typ {
        public BoolTyp() {
            this.typ = "Bool";
        }
        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Bool";
        }
    }

    public static class IntTyp extends Typ {
        public IntTyp() {
            this.typ = "Int";
        }
        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Int";
        }
    }

    public static class StringTyp extends Typ {
        public StringTyp() {
            this.typ = "String";
        }
        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toString() {
            return "String";
        }
    }

    public static class VoidTyp extends Typ {
        public VoidTyp() {
            this.typ = "Void";
        }
        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Void";
        }
    }

    public static class NullTyp extends Typ {
        public NullTyp() {
            this.typ = "Null";
        }
        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o) ||
                    o instanceof StringTyp ||
                    o instanceof ClasTyp;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Null";
        }
    }

    public static class ClasTyp extends Typ {
        public final String cname;

        public ClasTyp(String cname) {
            this.typ = "Class";
            this.cname = cname;
        }

        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return cname.equals(((ClasTyp) obj).cname);
        }

        @Override
        public String toString() {
            return cname;
        }
    }

    public static class FuncTyp extends Typ {
        public final List<Typ> argTyps;
        public final Typ retTyp;

        public FuncTyp(Ast.MdDecl mdDecl) {
            this.typ = "Func";
            this.argTyps = new ArrayList<>();
            for (Ast.VarDecl varDecl : mdDecl.args) {
                this.argTyps.add(varDecl.type);
            }
            this.retTyp = mdDecl.retTyp;
        }

        public FuncTyp(List<Typ> argTyps, Typ retTyp) {
            this.argTyps = argTyps;
            this.retTyp = retTyp;
        }

        @Override
        public boolean isSubTypeOrEquals(Typ o) {
            return equals(o);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return this.argTyps.equals(((FuncTyp) obj).argTyps) &&
                    this.retTyp.equals(((FuncTyp) obj).retTyp);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            StringJoiner sj = new StringJoiner(",");
            for (Typ arg : argTyps) {
                sj.add(arg.toString());
            }
            sb.append("[")
                    .append(sj.toString())
                    .append("]->")
                    .append(retTyp.toString());
            return sb.toString();
        }
    }

    public static abstract class Stmt extends Locatable implements Printable {
    }

    public static class IfStmt extends Stmt {
        public final Expr cond;
        public final List<Stmt> thenStmtList;
        public final List<Stmt> elseStmtList;

        public IfStmt(Expr cond, List<Stmt> thenStmtList, List<Stmt> elseStmtList) {
            this.cond = cond;
            this.thenStmtList = Collections.unmodifiableList(new ArrayList<>(thenStmtList));
            this.elseStmtList = Collections.unmodifiableList(new ArrayList<>(elseStmtList));
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i++);
            sb.append("if (");
            sb.append(cond.print());
            sb.append(") {\n");
            for (Stmt s : thenStmtList) {
                sb.append(s.print(i))
                        .append("\n");
            }
            indent(sb, --i);
            sb.append("}");
            if (!elseStmtList.isEmpty()) {
                sb.append(" else {\n");
                i++;
                for (Stmt s : elseStmtList) {
                    sb.append(s.print(i)).append("\n");
                }
                indent(sb, --i);
                sb.append("}\n");
            }
            return sb.toString();
        }
    }

    public static class WhileStmt extends Stmt {
        public final Expr cond;
        public final List<Stmt> stmtList;

        public WhileStmt(Expr cond, List<Stmt> stmtList) {
            this.cond = cond;
            this.stmtList = Collections.unmodifiableList(new ArrayList<>(stmtList));
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i++);
            sb.append("while (");
            sb.append(cond.print());
            sb.append(") {\n");
            for (Stmt s : stmtList) {
                sb.append(s.print(i))
                        .append("\n");
            }
            indent(sb, --i);
            sb.append("}");
            return sb.toString();
        }
    }


    public static class ReadlnStmt extends Stmt {
        public final String ident;

        public ReadlnStmt(String ident) {
            this.ident = ident;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("readln(")
                    .append(ident)
                    .append(")")
                    .append(";");
            return sb.toString();
        }
    }

    public static class PrintlnStmt extends Stmt {
        public final Expr expr;

        public PrintlnStmt(Expr expr) {
            this.expr = expr;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("println(")
                    .append(expr.print())
                    .append(")")
                    .append(";");
            return sb.toString();
        }
    }

    public static class VarAssignStmt extends Stmt {
        public Ast.VarDecl var; // resolved during typechecking phase
        public final String lhs;
        public final Expr rhs;

        public VarAssignStmt(String lhs, Expr rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(lhs)
                    .append(" = ")
                    .append(rhs.print())
                    .append(";");
            return sb.toString();
        }
    }

    public static class FieldAssignStmt extends Stmt {
        public final Expr lhsExpr;
        public final String lhsField;
        public final Expr rhs;

        public FieldAssignStmt(Expr lhsExpr, String lhsField, Expr rhs) {
            this.lhsExpr = lhsExpr;
            this.lhsField = lhsField;
            this.rhs = rhs;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(lhsExpr.print())
                    .append(".")
                    .append(lhsField)
                    .append(" = ")
                    .append(rhs.print())
                    .append(";");
            return sb.toString();
        }
    }

    public static class ReturnStmt extends Stmt {
        public final Expr expr;

        public ReturnStmt(Expr expr) {
            this.expr = expr;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("return ")
                    .append(expr.print())
                    .append(";");

            return sb.toString();
        }
    }

    public static class CallStmt extends Stmt {
        public final Expr target;
        public final List<Expr> args;

        public CallStmt(Expr target, List<Expr> args) {
            this.target = target;
            this.args = Collections.unmodifiableList(new ArrayList<>(args));
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(target.print())
                    .append("(");
            StringJoiner joiner = new StringJoiner(", ");

            for (Expr a : args) {
                joiner.add(a.print());
            }

            sb.append(joiner.toString())
                    .append(");");

            return sb.toString();
        }
    }

    public static abstract class Expr extends Locatable implements Printable {
        public Ast.Typ typ; // Inferred and annotated by StaticChecker
    }

    ;

    public static class StringLitExpr extends Expr {
        public final String str;

        public StringLitExpr(String str) {
            this.str = str;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("\"")
                    .append(str)
                    .append("\"");
            return sb.toString();
        }
    }

    public static class IntLitExpr extends Expr {
        public final int val;

        public IntLitExpr(int val) {
            this.val = val;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(val);
            return sb.toString();
        }
    }

    public static class BoolLitExpr extends Expr {
        public final boolean val;

        public BoolLitExpr(boolean val) {
            this.val = val;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            if (val) {
                sb.append("true");
            } else {
                sb.append("false");
            }
            return sb.toString();
        }
    }

    public static class NullLitExpr extends Expr {
        public NullLitExpr() {

        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("null");
            return sb.toString();
        }
    }

    public static class ThisExpr extends Expr {
        public ThisExpr() {

        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("this");
            return sb.toString();
        }
    }

    public static class IdentExpr extends Expr {
        public final String ident;

        public IdentExpr(String ident) {
            this.ident = ident;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(ident);
            return sb.toString();
        }
    }

    public static class UnaryExpr extends Expr {
        public final UnaryOp op;
        public final Expr expr;

        public UnaryExpr(UnaryOp op, Expr expr) {
            this.op = op;
            this.expr = expr;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(op.toString())
                    .append(expr.print());
            return sb.toString();
        }
    }

    public static class BinaryExpr extends Expr {
        public final BinaryOp op;
        public final Expr lhs;
        public final Expr rhs;

        public BinaryExpr(BinaryOp op, Expr lhs, Expr rhs) {
            this.op = op;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("(")
                    .append(lhs.print())
                    .append(" ")
                    .append(op.toString())
                    .append(" ")
                    .append(rhs.print())
                    .append(")");
            return sb.toString();
        }
    }

    public static class DotExpr extends Expr {
        public final Expr target;
        public final String ident;

        public DotExpr(Expr target, String ident) {
            this.target = target;
            this.ident = ident;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(target.print())
                    .append(".")
                    .append(ident);
            return sb.toString();
        }
    }

    public static class CallExpr extends Expr {
        public final Expr target;
        public final List<Expr> args;

        public CallExpr(Expr target, List<Expr> args) {
            this.target = target;
            this.args = args;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(target.print())
                    .append("(");

            StringJoiner joiner = new StringJoiner(", ");
            for (Expr a : args) {
                joiner.add(a.print());
            }
            sb.append(joiner.toString());
            sb.append(")");
            return sb.toString();
        }
    }

    public static class NewExpr extends Expr {
        public final String cname;

        public NewExpr(String cname) {
            this.cname = cname;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("new ")
                    .append(cname);
            return sb.toString();
        }
    }

    public static enum UnaryOp {
        NEGATIVE {
            public String toString() {
                return "-";
            }
        },
        NOT {
            public String toString() {
                return "!";
            }
        }
    }

    public static enum BinaryOp {
        PLUS {
            public String toString() {
                return "+";
            }
        },
        MINUS {
            public String toString() {
                return "-";
            }
        },
        MULT {
            public String toString() {
                return "*";
            }
        },
        DIV {
            public String toString() {
                return "/";
            }
        },
        LT {
            public String toString() {
                return "<";
            }
        },
        GT {
            public String toString() {
                return ">";
            }
        },
        GEQ {
            public String toString() {
                return ">=";
            }
        },
        EQ {
            public String toString() {
                return "==";
            }
        },
        LEQ {
            public String toString() {
                return "<=";
            }
        },
        NEQ {
            public String toString() {
                return "!=";
            }
        },
        AND {
            public String toString() {
                return "&&";
            }
        },
        OR {
            public String toString() {
                return "||";
            }
        }
    }

    public static class MdDecl extends Locatable implements Printable {
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

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(retTyp.toString())
                    .append(" ")
                    .append(name)
                    .append("(");
            // Args
            StringJoiner joiner = new StringJoiner(", ");
            for (VarDecl v : args) {
                joiner.add(v.print());
            }
            sb.append(joiner.toString());
            sb.append(") {\n");
            i++;

            // vars
            for (VarDecl v : vars) {
                sb.append(v.print(i)).append(";\n");
            }

            // stmts
            for (Stmt s : stmts) {
                sb.append(s.print(i))
                        .append("\n");
            }

            indent(sb, --i);
            sb.append("}");

            return sb.toString();
        }
    }

    public static void main(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String fileLoc = argv[i];

            try {
                Ast.Prog prog = jlite.parser.parser.parse(fileLoc);
                System.out.println(prog.toJSON());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
