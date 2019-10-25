package jlite.ir;

import jlite.parser.Ast;

import java.util.ArrayList;
import java.util.StringJoiner;

public class Ir3 {
    public static void indent(StringBuilder sb, int i) {
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
        String ident;

        public ReadlnStmt(String ident) {
            this.ident = ident;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("readln(")
                    .append(ident)
                    .append(");");
            return sb.toString();
        }
    }

    public static class PrintlnStmt extends Stmt {
        Var var;

        public PrintlnStmt(Var v) {
            super();
            this.var = v;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("println(")
                    .append(this.var.name)
                    .append(");");
            return sb.toString();
        }
    }

    public static class AssignStmt extends Stmt {
        Var var;
        String res;

        public AssignStmt(Var v, int val) {
            super();
            this.var = v;
            this.res = String.valueOf(val);
        }

        public AssignStmt(Var v, String str) {
            super();

            this.var = v;
            this.res = str;
        }

        public AssignStmt(Var v, boolean val) {
            super();
            this.var = v;
            this.res = val ? "true" : "false";
        }

        public AssignStmt(Var v, Expr3 expr3) {
            super();
            this.var = v;
            this.res = expr3.print();
        }

        public AssignStmt(Var v, Rval rv) {
            super();
            assert rv instanceof Var;
            this.var = v;
            this.res = ((Var) rv).name;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(String.format("%s = %s;", var.name, res));
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
}
