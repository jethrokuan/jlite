package jlite.arm;

import jlite.ir.Ir3;

import java.util.*;

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

        public String print(int i) {
            return name;
        }
    }

    public interface Printable {
        default String print() {
            return this.print(0);
        }

        String print(int i);
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
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            for (String global : globals) {
                sb.append("\t.global ").append(global).append("\n");
            }
            sb.append("\n\t.text\n");
            for (Block text : textList) {
                sb.append(text.print(i));
            }

            sb.append("\n\t.data\n");
            for (Block data : dataList) {
                sb.append(data.print(i));
            }
            return sb.toString();
        }
    }

    public static class Block implements Printable {
        public String name;
        public boolean isPrologue;
        public ArrayList<ArmIsn> armIsns;

        public Block(String name, ArrayList<ArmIsn> armIsns) {
            this.name = name;
            this.armIsns = armIsns;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
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

    private static abstract class ArmIsn implements Printable {
    }

    public static class PushIsn extends ArmIsn {
        Set<Reg> regs;

        public PushIsn(HashSet<Reg> regs) {
            super();
            this.regs = regs;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            indent(sb, i);
            ArrayList<Reg> sortedReg = new ArrayList<>();
            for (Reg reg : regs) {
                sortedReg.add(reg);
            }
            Collections.sort(sortedReg);
            sb.append("push {");
            StringJoiner joiner = new StringJoiner(", ");
            for (Arm.Reg reg : sortedReg) {
                joiner.add(reg.print());
            }
            sb.append(joiner.toString())
                    .append("}");
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
        public String print(int i) {
            return String.format("\tsub %s, %s, %s", dst.print(), lhs.print(), rhs.print());
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

        @Override
        public String print(int i) {
            return "#" + i;
        }
    }

    public static class Op2Reg extends Op2 {
        public Reg reg;

        public Op2Reg(Reg reg) {
            this.reg = reg;
        }

        @Override
        public String print(int i) {
            return reg.print();
        }
    }

    public static class BIsn extends ArmIsn {
        String label;

        public BIsn(String label) {
            super();
            this.label = label;
        }

        @Override
        public String print(int i) {
            return String.format("\tb %s", label);
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
        public String print(int i) {
            return String.format("\tadd %s, %s, %s", dst.print(), lhs.print(), rhs.print());
        }
    }

    public static class PopIsn extends ArmIsn {
        Set<Reg> regs;

        public PopIsn(HashSet<Reg> regs) {
            super();
            this.regs = regs;
        }

        @Override
        public String print(int i) {
            StringBuilder sb = new StringBuilder();
            ArrayList<Reg> sortedRegs = new ArrayList<>();
            for (Reg reg : regs) {
                sortedRegs.add(reg);
            }
            Collections.sort(sortedRegs);
            StringJoiner joiner = new StringJoiner(", ");
            for (Reg reg : sortedRegs) {
                joiner.add(reg.print());
            }
            sb.append("\t pop {")
                    .append(joiner.toString())
                    .append("}");
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
        public String print(int i) {
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
        public String print(int i) {
            return String.format("\tldr %s, =%s\n", reg.print(), i);
        }
    }
}
