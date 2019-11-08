package jlite.ir;

import jlite.parser.Ast;

import java.util.ArrayList;
import java.util.StringJoiner;

public class Ir3 {
    private static void indent(StringBuilder sb, int i) {
        while (i > 0) {
            sb.append("  ");
            i--;
        }
    }

    public interface Printable {
        default String print() {
            return this.print(0);
        }

        String print(int indent);
    }

    public static class Prog implements Printable {
        ArrayList<Data> datas;
        ArrayList<Method> methods;

        public Prog(ArrayList<Data> datas, ArrayList<Method> methods) {
            this.datas = datas;
            this.methods = methods;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("======= CData3 =======\n");
            for (Data data : datas) {
                sb.append(data.print(i))
                        .append("\n\n");
            }
            sb.append("======= CMtd3 =======\n");
            for (Method method : methods) {
                sb.append(method.print(i))
                        .append("\n");
            }
            sb.append("=====fx== End of IR3 Program =======\n");
            return sb.toString();
        }
    }

    public static class DataField implements Printable {
        Ast.Typ typ;
        String ident;

        public DataField(Ast.Typ type, String ident) {
            this.typ = type;
            this.ident = ident;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(String.format("%s %s;", typ, ident));
            return sb.toString();
        }
    }

    public static class Data implements Printable {
        String cname;
        ArrayList<DataField> fields;

        public Data(String cname, ArrayList<DataField> fields) {
            this.cname = cname;
            this.fields = fields;
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("Data3 ")
                    .append(cname)
                    .append("{\n");
            i++;
            for (DataField field : fields) {
                sb.append(field.print(i))
                        .append("\n");
            }
            i--;
            sb.append("}");
            return sb.toString();
        }
    }

    public static class Method implements Printable {
        public ArrayList<Var> args;
        public ArrayList<Var> locals;
        public ArrayList<Stmt> statements;

        String name;
        Ast.Typ retTyp;

        public Method(String name, Ast.Typ retTyp) {
            this.name = name;
            this.retTyp = retTyp;
            this.args = new ArrayList<>();
            this.locals = new ArrayList<>();
            this.statements = new ArrayList<>();
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(retTyp.toString())
                    .append(" ")
                    .append(name)
                    .append("(");
            StringJoiner joiner = new StringJoiner(", ");
            for (Var arg : args) {
                joiner.add(arg.print());
            }
            sb.append(joiner.toString());
            sb.append("){\n");
            i++;
            for (Var local : locals) {
                indent(sb, i);
                sb.append(String.format("%s %s;\n", local.typ, local.name));
            }

            for (Stmt stmt : statements) {
                sb.append(stmt.print(i))
                        .append("\n");
            }
            i--;
            sb.append("}\n");
            return sb.toString();
        }
    }

    public static class Var extends Rval implements Printable {
        Ast.Typ typ;
        String name;

        public Var(Ast.Typ typ, String name) {
            this.typ = typ;
            this.name = name;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(name);
            return sb.toString();
        }

        @Override
        public Ast.Typ getTyp() {
            return typ;
        }
    }

    public static abstract class Stmt implements Printable {
    }

    public static class ReadlnStmt extends Stmt {
        Var var;

        public ReadlnStmt(Var var) {
            this.var = var;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("readln(")
                    .append(var.print())
                    .append(");");
            return sb.toString();
        }
    }

    public static class PrintlnStmt extends Stmt {
        Rval rval;

        public PrintlnStmt(Rval rval) {
            super();
            this.rval = rval;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("println(")
                    .append(this.rval.print())
                    .append(");");
            return sb.toString();
        }
    }

    public static class AssignStmt extends Stmt {
        Var var;
        Rval rval;
        Expr3 expr;

        public AssignStmt(Var v, Expr3 expr3) {
            super();
            this.var = v;
            this.expr = expr3;
            this.rval = null;
        }

        public AssignStmt(Var v, Rval rv) {
            super();
            this.var = v;
            this.rval = rv;
            this.expr = null;
        }

        @Override
        public String print(int i) {
            assert (this.rval != null || this.expr != null);
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            StringBuilder output = new StringBuilder();
            if (this.rval != null) {
                output.append(this.rval.print());
            } else if (this.expr != null) {
                output.append(this.expr.print());
            }

            sb.append(String.format("%s = %s;", var.name, output.toString()));
            return sb.toString();
        }
    }

    public abstract static class Rval implements Printable {
        public abstract Ast.Typ getTyp();
    }

    public abstract static class Expr3 implements Printable {

    }

    public static class ReturnStmt extends Stmt implements Printable {
        Rval rv;

        public ReturnStmt(Rval rv) {
            super();
            this.rv = rv;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);

            sb.append("return ");
            if (rv != null) {
                sb.append(rv.print());
            }
            sb.append(";");
            return sb.toString();
        }
    }

    public static class BinaryExpr extends Expr3 implements Printable {
        public Rval lhs;
        public Rval rhs;
        public Ast.BinaryOp op;

        public BinaryExpr(Ast.BinaryOp op, Rval lhs, Rval rhs) {
            super();
            this.op = op;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(lhs.print())
                    .append(" ")
                    .append(op.toString())
                    .append(" ")
                    .append(rhs.print());
            return sb.toString();
        }
    }

    public static class LabelStmt extends Stmt implements Printable {
        String label;

        public LabelStmt(String label) {
            this.label = label;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(label).append(":");
            return sb.toString();
        }
    }

    public static abstract class JumpStmt extends Stmt implements Printable {
        public LabelStmt label;

        public abstract void setLabel(LabelStmt label);

    }

    public static class GotoStmt extends JumpStmt {
        public GotoStmt(LabelStmt label) {
            super();
            this.label = label;
        }

        public GotoStmt() {
            this.label = new Ir3.LabelStmt("___");
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("goto ")
                    .append(label.label)
                    .append(";");
            return sb.toString();
        }

        public void setLabel(LabelStmt label) {
            this.label = label;
        }
    }

    public static class CmpStmt extends JumpStmt {
        Ast.BinaryOp op;
        Rval lRv;
        Rval rRv;
        LabelStmt label;

        public CmpStmt(Ast.BinaryOp op, Rval lRv, Rval rRv, LabelStmt label) {
            super();
            this.op = op;
            this.lRv = lRv;
            this.rRv = rRv;
            this.label = label;
        }

        public CmpStmt(Ast.BinaryOp op, Rval lRv, Rval rRv) {
            super();
            this.op = op;
            this.lRv = lRv;
            this.rRv = rRv;
            this.label = new Ir3.LabelStmt("null");
        }

        public void setLabel(LabelStmt label) {
            this.label = label;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("if (")
                    .append(lRv.print())
                    .append(" ")
                    .append(op)
                    .append(" ")
                    .append(rRv.print())
                    .append(") goto ")
                    .append(label.label)
                    .append(";");
            return sb.toString();
        }
    }

    public static class IntConst extends Rval {
        int val;

        public IntConst(int i) {
            super();
            this.val = i;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(val);
            return sb.toString();
        }

        @Override
        public Ast.Typ getTyp() {
            return new Ast.IntTyp();
        }
    }

    public static class NewExpr extends Expr3 implements Printable {
        Data data;

        public NewExpr(Data data) {
            super();
            this.data = data;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("new ");
            sb.append(data.cname)
                    .append("()");
            return sb.toString();
        }
    }

    public static class IntRval extends Rval {
        Integer i;

        public IntRval(Integer i) {
            this.i = i;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(this.i);
            return sb.toString();
        }

        @Override
        public Ast.Typ getTyp() {
            return new Ast.IntTyp();
        }
    }

    public static class StringRval extends Rval {
        String s;

        public StringRval(String s) {
            this.s = s;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(this.s);
            return sb.toString();
        }

        @Override
        public Ast.Typ getTyp() {
            return new Ast.StringTyp();
        }
    }

    public static class BoolRval extends Rval {
        boolean b;

        public BoolRval(boolean b) {
            this.b = b;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            String s = this.b ? "true" : "false";
            sb.append(s);
            return sb.toString();
        }

        @Override
        public Ast.Typ getTyp() {
            return new Ast.BoolTyp();
        }
    }

    public static class FieldAssignStatement extends Stmt {
        Var target;
        String field;
        Rval v;

        public FieldAssignStatement(Var target, String field, Rval v) {
            super();
            this.target = target;
            this.field = field;
            this.v = v;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(target.print())
                    .append(".")
                    .append(field)
                    .append(" = ")
                    .append(v.print())
                    .append(";");
            return sb.toString();
        }
    }

    public static class CallStmt extends Stmt {
        Var lhs;
        Method method;
        ArrayList<Rval> args;

        public CallStmt(Var lhs, Method irMethod, ArrayList<Rval> args) {
            this.lhs = lhs;
            this.method = irMethod;
            this.args = args;
        }

        public CallStmt(Method irMethod, ArrayList<Rval> args) {
            this.lhs = null;
            this.method = irMethod;
            this.args = args;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            StringJoiner sj = new StringJoiner(", ");
            for (Rval arg : args) {
                sj.add(arg.print());
            }

            if (lhs != null) {
                sb.append(lhs.print())
                        .append(" = ");
            }

            sb.append(method.name)
                    .append("(")
                    .append(sj.toString())
                    .append(");");
            return sb.toString();
        }
    }

    public static class FieldAccessStatement extends Stmt {
        Var lhs;
        Var target;
        String field;

        public FieldAccessStatement(Var lhs, Var target, String field) {
            super();
            this.lhs = lhs;
            this.target = target;
            this.field = field;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(lhs.print())
                    .append(" = ")
                    .append(target.print())
                    .append(".")
                    .append(field)
                    .append(";");
            return sb.toString();
        }
    }

    public static class UnaryStmt extends Stmt {
        Var lhs;
        Ast.UnaryOp op;
        Rval rhs;

        public UnaryStmt(Var lhs, Ast.UnaryOp op, Rval rhs) {
            super();
            this.lhs = lhs;
            this.op = op;
            this.rhs = rhs;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(lhs.print())
                    .append(" = ")
                    .append(op)
                    .append(rhs.print())
                    .append(";");
            return sb.toString();
        }
    }
}
