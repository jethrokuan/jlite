package jlite.ir;

import jlite.parser.Ast;

import java.util.*;
import java.util.stream.Collectors;

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
        public ArrayList<Data> datas;
        public ArrayList<Method> methods;

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
        public String cname;
        public ArrayList<DataField> fields;

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
        public ArrayList<Block> blocks;
        public ArrayList<Block> blockPreOrder;
        public ArrayList<Block> blockPostOrder;
        public DominanceInfo dominance;
        public ArrayList<Web> webs;
        public LivenessInfo liveness;

        public String name;
        Ast.Typ retTyp;

        public Method(String name, Ast.Typ retTyp) {
            this.name = name;
            this.retTyp = retTyp;
            this.args = new ArrayList<>();
            this.locals = new ArrayList<>();
            this.statements = new ArrayList<>();
            this.blocks = null;
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

            if (blocks != null) {
                sb.append(printBlocks(i));
            } else {
                sb.append(printStatements(i));
            }
            i--;
            sb.append("}\n");
            return sb.toString();
        }

        private String printBlocks(int i) {
            StringBuilder sb = new StringBuilder();
            for (Block b : blocks) {
                sb.append(b.print(i));
            }
            return sb.toString();
        }

        public String printStatements(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            for (Stmt stmt : statements) {
                sb.append(stmt.print(i))
                        .append("\n");
            }
            return sb.toString();
        }
    }

    public static class Var implements Printable {
        public Ast.Typ typ;
        public String name;
        public Web web;
        public boolean spilled = false;
        public Integer reg = -1;

        public Var(Ast.Typ typ, String name) {
            this.typ = typ;
            this.name = name;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(name);
            if (reg >= 0) {
                sb.append(" {r")
                        .append(reg)
                        .append("}");
            }
            return sb.toString();
        }
    }

    public static abstract class Stmt implements Printable {
        public List<Var> getDefs() {
            return Collections.emptyList();
        }

        public List<Var> getUses() {
            return getRvals().stream()
                    .filter(rval -> rval instanceof Ir3.VarRval)
                    .map(rval -> ((VarRval) rval).var)
                    .collect(Collectors.toList());
        }

        public abstract List<Rval> getRvals();

        public abstract void updateDef(Var newVar);
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

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(var);
        }

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            var = newVar;
        }
    }

    public static class PrintlnStmt extends Stmt {
        public Rval rval;

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

        @Override
        public List<Rval> getRvals() {
            return Arrays.asList(rval);
        }

        @Override
        public void updateDef(Var newVar) {
            System.out.println("Can't update var in println statement");
        }
    }

    public static class AssignStmt extends Stmt {
        public Var var;
        public Rval rval;
        public Expr3 expr;

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

            sb.append(String.format("%s = %s;", var.print(), output.toString()));
            return sb.toString();
        }

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(var);
        }

        @Override
        public List<Rval> getRvals() {
            if (rval != null) {
                return Arrays.asList(rval);
            } else {
                return expr.getRvals();
            }
        }

        @Override
        public void updateDef(Var newVar) {
            var = newVar;
        }
    }

    public abstract static class Rval implements Printable {
        public abstract Ast.Typ getTyp();
    }

    public abstract static class Expr3 implements Printable {

        public abstract List<Rval> getRvals();
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

        @Override
        public List<Rval> getRvals() {
            if (rv == null) {
                return Collections.emptyList();
            } else {
                return Arrays.asList(rv);
            }
        }

        @Override
        public void updateDef(Var newVar) {
            System.out.println("Can't update var in return statement");
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

        @Override
        public List<Rval> getRvals() {
            return Arrays.asList(lhs, rhs);
        }
    }

    public static class LabelStmt extends Stmt implements Printable {
        public String label;

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

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            System.out.println("Can't update var in label statement");
        }
    }

    public static abstract class JumpStmt extends Stmt implements Printable {
        public LabelStmt label;

        public abstract void setLabel(LabelStmt label);

        public LabelStmt getLabel() {
            return label;
        }

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

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            System.out.println("Can't update var in jump statement");
        }
    }

    public static class CmpStmt extends JumpStmt {
        public Ast.BinaryOp op;
        public Rval lRv;
        public Rval rRv;

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

        @Override
        public List<Rval> getRvals() {
            return Arrays.asList(lRv, rRv);
        }

        @Override
        public void updateDef(Var newVar) {
            System.out.println("Can't update var in cmp statement");
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

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }
    }

    public static class IntRval extends Rval {
        public Integer i;

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

    public static class VarRval extends Rval {
        public Var var;

        public VarRval(Var var) {
            this.var = var;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(var.print());
            return sb.toString();
        }

        @Override
        public Ast.Typ getTyp() {
            return var.typ;
        }
    }


    public static class FieldAssignStatement extends Stmt {
        public Var target;
        public String field;
        public Rval v;

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

        @Override
        public List<Rval> getRvals() {
            return Arrays.asList(v);
        }

        @Override
        public void updateDef(Var newVar) {
            target = newVar;
        }
    }

    public static class CallStmt extends Stmt {
        public Var lhs;
        public Method method;
        public ArrayList<Rval> args;

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

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(lhs);
        }

        @Override
        public List<Rval> getRvals() {
            return args;
        }

        @Override
        public void updateDef(Var newVar) {
            lhs = newVar;
        }
    }

    public static class FieldAccessStatement extends Stmt {
        public Var lhs;
        public Var target;
        public String field;

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

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(lhs);
        }

        @Override
        public List<Rval> getRvals() {
            return Arrays.asList(new VarRval(target));
        }

        @Override
        public void updateDef(Var newVar) {
            lhs = newVar;
        }
    }

    public static class UnaryStmt extends Stmt {
        public Var lhs;
        public Ast.UnaryOp op;
        public Rval rhs;

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

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(lhs);
        }

        @Override
        public List<Rval> getRvals() {
            return Arrays.asList(rhs);
        }

        @Override
        public void updateDef(Var newVar) {
            lhs = newVar;
        }
    }

    public static class Block implements Printable {
        public LabelStmt labelStmt;
        public ArrayList<Ir3.Stmt> statements;
        public ArrayList<Block> outgoing = new ArrayList<>();
        public ArrayList<Block> incoming = new ArrayList<>();
        public int preOrderIndex;
        public int postOrderIndex;

        public Block() {
            statements = new ArrayList<>();
        }

        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(labelStmt.print())
                    .append("\n");
            i++;
            for (Stmt stmt : statements) {
                sb.append(stmt.print(i)).append("\n");
            }
            i--;
            return sb.toString();
        }
    }

    public static class PhiStmt extends Stmt {
        public ArrayList<Var> args;
        public Var var;
        public boolean memory; // whether it resides in memory

        public PhiStmt(Var v, int size) {
            super();
            this.var = v;
            this.args = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                this.args.add(new Var(new Ast.StringTyp(), "UNSET"));
            }
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            StringJoiner joiner = new StringJoiner(", ");
            for (Var arg : args) {
                joiner.add(arg.print());
            }
            sb.append(var.print())
                    .append(" = phi(")
                    .append(joiner.toString())
                    .append(")");
            return sb.toString();
        }

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(var);
        }

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            var = newVar;
        }
    }

    public static class Web {
    }

    public static class LoadStmt extends Stmt {
        Var var;

        public LoadStmt(Var toSpill) {
            super();
            this.var = toSpill;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append(var.print())
                    .append(" = load MEM_")
                    .append(var.name)
                    .append(";");
            return sb.toString();
        }

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            this.var = newVar;
        }

        @Override
        public List<Var> getDefs() {
            return Arrays.asList(var);
        }
    }

    public static class StoreStmt extends Stmt {
        public Var var;

        public StoreStmt(Var toSpill) {
            super();
            this.var = toSpill;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("MEM_")
                    .append(var.name)
                    .append(" = store ")
                    .append(var.print())
                    .append(";");
            return sb.toString();
        }

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            this.var = newVar;
        }

        @Override
        public List<Var> getUses() {
            return Arrays.asList(var);
        }
    }

    public static class StackArgStmt extends Stmt {
        public Var var;
        public int loc;

        public StackArgStmt(Var v, int i) {
            super();
            this.var = v;
            this.loc = i;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            sb.append("stack(")
                    .append(var.print())
                    .append(", ")
                    .append(loc)
                    .append(");");
            return sb.toString();
        }

        @Override
        public List<Rval> getRvals() {
            return Collections.emptyList();
        }

        @Override
        public void updateDef(Var newVar) {
            return;
        }

        @Override
        public List<Var> getUses() {
            return Arrays.asList(var);
        }
    }

    public static class PrintfStmt extends Stmt {
        public ArrayList<Rval> args;

        public PrintfStmt(ArrayList<Rval> args) {
            super();
            this.args = args;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            StringJoiner joiner = new StringJoiner(", ");
            for (Ir3.Rval arg : args) {
                joiner.add(arg.print());
            }
            sb.append("printf(")
                    .append(joiner.toString())
                    .append(");");
            return sb.toString();
        }

        @Override
        public List<Rval> getRvals() {
            return args;
        }

        @Override
        public void updateDef(Var newVar) {
            return;
        }
    }

    public class NullRval extends Rval {
        @Override
        public String print(int indent) {
            return "null";
        }

        @Override
        public Ast.Typ getTyp() {
            return new Ast.NullTyp();
        }
    }
}
