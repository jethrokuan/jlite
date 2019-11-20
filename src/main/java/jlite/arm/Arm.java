package jlite.arm;

import jlite.ir.Ir3;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Arm {
    public static boolean isConstant(Ir3.Rval rv) {
        return (rv instanceof Ir3.IntRval) || (rv instanceof Ir3.StringRval) || (rv instanceof Ir3.BoolRval) || (rv instanceof Ir3.NullRval);
    }

    public static boolean isValidOp2(Ir3.Rval rv) {
        return (rv instanceof Ir3.NullRval) ||
                (rv instanceof Ir3.BoolRval);
    }

    private static void indent(StringBuilder sb, int i) {
        while (i > 0) {
            sb.append("  ");
            i--;
        }
    }

    public static int toInt(Ir3.Rval rv) {
        if (rv instanceof Ir3.IntRval) {
            return ((Ir3.IntRval) rv).i;
        } else if (rv instanceof Ir3.BoolRval) {
            return ((Ir3.BoolRval) rv).b ? 1 : 0;
        } else if (rv instanceof Ir3.NullRval) {
            return 0;
        } else {
            throw new AssertionError("Can't convert rv of type " + rv.getTyp() + " to int.");
        }
    }

    public static enum Reg implements Printable {
        R0("r0"),
        R1("r1"),
        R2("r2"),
        R3("r3"),
        R4("r4"),
        R5("r5"),
        R6("r6"),
        R7("r7"),
        R8("r8"),
        R9("r9"),
        R10("r10"),
        R11("r11"),
        R12("r12"),
        SP("sp"),
        LR("lr"),
        PC("pc");

        private final String name;

        private Reg(String name) {
            this.name = name;
        }

        public static Reg fromInt(int x) {
            switch (x) {
                case 0:
                    return R0;
                case 1:
                    return R1;
                case 2:
                    return R2;
                case 3:
                    return R3;
                case 4:
                    return R4;
                case 5:
                    return R5;
                case 6:
                    return R6;
                case 7:
                    return R7;
                case 8:
                    return R8;
                case 9:
                    return R9;
                case 10:
                    return R10;
                case 11:
                    return R11;
                case 12:
                    return R12;
            }
            throw new AssertionError("Invalid register number " + x);
        }

        @Override
        public String print() {
            return name;
        }
    }

    public enum Cond implements Printable {
        EQ("eq"), // Equal
        NE("ne"), // Not equal
        GE("ge"), // Greater than or equal, signed
        LT("lt"), // Less than, signed
        GT("gt"), // Greater than, signed
        LE("le"), // Less than or equal, signed
        AL(""); // Can have any value

        private final String suffix;

        Cond(String suffix) {
            this.suffix = suffix;
        }

        public String print() {
            return suffix;
        }
    }
    public interface Printable {
        String print();
    }

    public static class Prog implements Printable {
        public ArrayList<Block> textList;
        public ArrayList<Block> dataList;
        public ArrayList<String> globals;

        public Prog(ArrayList<Block> textList, ArrayList<Block> dataList, ArrayList<String> globals) {
            this.textList = textList;
            this.dataList = dataList;
            this.globals = globals;
        }

        @Override
        public String print() {
            StringBuilder sb = new StringBuilder();
            for (String global : globals) {
                sb.append("\t.global ").append(global).append("\n");
            }
            sb.append("\n\t.text\n");
            for (Block text : textList) {
                sb.append(text.print());
            }

            sb.append("\n\t.data\n");
            for (Block data : dataList) {
                sb.append(data.print());
            }
            return sb.toString();
        }
    }

    public static class Block implements Printable {
        public String name;
        public boolean isPrologue;
        public List<ArmIsn> armIsns;

        public Block(String name, List<ArmIsn> armIsns) {
            this.name = name;
            this.armIsns = armIsns;
        }

        @Override
        public String print() {
            StringBuilder sb = new StringBuilder();
            if (isPrologue) {
                sb.append("\n");
            }
            sb.append(name).append(":\n");
            for (ArmIsn instr : armIsns) {
                sb.append(instr.print());
            }
            return sb.toString();
        }
    }

    public static abstract class ArmIsn implements Printable {
    }

    public static class PushIsn extends ArmIsn {
        List<Reg> regs;

        public PushIsn(List<Reg> regs) {
            super();
            this.regs = new ArrayList<>();
            for (Arm.Reg reg : regs) {
                this.regs.add(reg);
            }
        }

        @Override
        public String print() {
            StringBuilder sb = new StringBuilder();
            sb.append("\tpush {");
            StringJoiner joiner = new StringJoiner(", ");
            for (Arm.Reg reg : regs) {
                joiner.add(reg.print());
            }
            sb.append(joiner.toString())
                    .append("}\n");
            return sb.toString();
        }
    }

    public static class SubIsn extends ArmIsn {
        public Reg dst;
        public Reg lhs;
        public Op2 rhs;

        public SubIsn(Reg dst, Reg lhs, Arm.Op2 rhs) {
            super();
            this.dst = dst;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print() {
            return String.format("\tsub %s, %s, %s\n", dst.print(), lhs.print(), rhs.print());
        }
    }

    public static abstract class Op2 implements Printable {
    }

    public static class Op2Const extends Op2 {
        public int i;

        public Op2Const(int i) {
            super();
            this.i = i;
        }

        public Op2Const(Ir3.Rval rv) {
            super();
            assert Arm.isConstant(rv);
            if (rv instanceof Ir3.IntRval) {
                Ir3.IntRval intRval = (Ir3.IntRval) rv;
                this.i = intRval.i;
            } else if (rv instanceof Ir3.BoolRval) {
                Ir3.BoolRval boolRval = (Ir3.BoolRval) rv;
                this.i = boolRval.b ? 1 : 0;
            } else if (rv instanceof Ir3.NullRval) {
                this.i = 0;
            } else {
                throw new AssertionError("Invalid rv " + rv.print());
            }
        }

        @Override
        public String print() {
            return "#" + i;
        }
    }

    public static class Op2Reg extends Op2 {
        public Reg reg;

        public Op2Reg(Reg reg) {
            this.reg = reg;
        }

        @Override
        public String print() {
            return reg.print();
        }
    }

    public static class BIsn extends ArmIsn {
        String label;
        Cond op;

        public BIsn(String label) {
            super();
            this.label = label;
            this.op = Cond.AL;
        }

        public BIsn(Cond op, String label) {
            super();
            this.op = op;
            this.label = label;
        }

        @Override
        public String print() {
            return String.format("\tb%s %s\n", op.print(), label);
        }
    }

    public static class AddIsn extends ArmIsn {
        public Reg dst;
        public Reg lhs;
        public Op2 rhs;

        public AddIsn(Reg dst, Reg lhs, Op2 rhs) {
            super();
            this.dst = dst;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print() {
            return String.format("\tadd %s, %s, %s\n", dst.print(), lhs.print(), rhs.print());
        }
    }

    public static class PopIsn extends ArmIsn {
        List<Reg> regs;

        public PopIsn(List<Reg> regs) {
            super();
            this.regs = new ArrayList<>();
            for (Arm.Reg reg : regs) {
                this.regs.add(reg);
            }
        }

        @Override
        public String print() {
            StringBuilder sb = new StringBuilder();
            StringJoiner joiner = new StringJoiner(", ");
            for (Reg reg : regs) {
                joiner.add(reg.print());
            }
            sb.append("\tpop {")
                    .append(joiner.toString())
                    .append("}\n");
            return sb.toString();
        }
    }

    public static class BxIsn extends ArmIsn {
        Reg reg;

        public BxIsn(Reg reg) {
            super();
            this.reg = reg;
        }

        @Override
        public String print() {
            return String.format("\tbx %s\n", reg.print());
        }
    }

    public static class LdrConstIsn extends ArmIsn {
        public Reg reg;
        public int i;

        public LdrConstIsn(Reg reg, int i) {
            super();
            this.reg = reg;
            this.i = i;
        }

        @Override
        public String print() {
            return String.format("\tldr %s, =%s\n", reg.print(), this.i);
        }
    }

    public static class CmpIsn extends ArmIsn {
        Cond cond;
        Reg lhs;
        Op2 rhs;

        public CmpIsn(Reg lhs, Op2 rhs) {
            super();
            this.cond = Cond.AL;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print() {
            return String.format("\tcmp%s %s, %s\n", cond.print(), lhs.print(), rhs.print());
        }
    }

    /**
     * Like Ascii Isn, but makes string NUL terminated.
     */
    public static class AscizIsn extends ArmIsn {
        public String str;

        public AscizIsn(String s) {
            this.str = s;
        }

        @Override
        public String print() {
            return String.format("\t.asciz \"%s\"\n", str);
        }
    }

    public static class LdrLabelIsn extends ArmIsn {
        public Reg reg;
        public String label;

        public LdrLabelIsn(Reg reg, String label) {
            super();
            this.reg = reg;
            this.label = label;
        }

        @Override
        public String print() {
            return String.format("\tldr %s, =%s\n", reg.print(), label);
        }
    }

    public static class MovIsn extends ArmIsn {
        private final Op2 rhs;
        private final Reg lhs;

        public MovIsn(Reg lhs, Op2 rhs) {
            super();
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print() {
            return String.format("\tmov %s, %s\n", lhs.print(), rhs.print());
        }
    }

    public static class MulIsn extends ArmIsn {
        public Reg dst;
        public Reg lhs;
        public Reg rhs;

        public MulIsn(Reg dst, Reg lhs, Reg rhs) {
            super();
            this.dst = dst;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print() {
            return String.format("\tmul %s, %s, %s\n", dst.print(), lhs.print(), rhs.print());
        }
    }

    public static class RsbIsn extends ArmIsn {
        public Reg dst;
        public Reg lhs;
        public Op2 rhs;

        public RsbIsn(Reg dst, Reg lhs, Op2 rhs) {
            super();
            this.dst = dst;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String print() {
            return String.format("\trsb %s, %s, %s\n", dst.print(), lhs.print(), rhs.print());
        }
    }

    public static class StrIsn extends ArmIsn {
        public Reg dst;
        public Reg rhs;
        public int offset;

        public StrIsn(Reg dst, Reg rhs, int offset) {
            super();
            this.dst = dst;
            this.rhs = rhs;
            this.offset = offset;
        }

        @Override
        public String print() {
            if (offset == 0) {
                return String.format("\tstr %s, [%s]\n", dst.print(), rhs.print());
            } else {
                return String.format("\tstr %s, [%s, #%s]\n", dst.print(), rhs.print(), offset);
            }
        }
    }

    public static class BlIsn extends ArmIsn {
        public String label;

        public BlIsn(String label) {
            super();
            this.label = label;
        }

        @Override
        public String print() {
            return String.format("\tbl %s\n", label);
        }
    }

    public static class LdrIsn extends ArmIsn {
        public Reg dst;
        public Reg rhs;
        public int offset;

        public LdrIsn(Reg dst, Reg rhs, int offset) {
            super();
            this.dst = dst;
            this.rhs = rhs;
            this.offset = offset;
        }

        @Override
        public String print() {
            return String.format("\tldr %s, [%s, #%s]\n", dst.print(), rhs.print(), offset);
        }
    }
}
