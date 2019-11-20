package jlite.runners;

public class Ast {
    public static void main(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String fileLoc = argv[i];

            try {
                jlite.parser.Ast.Prog prog = jlite.parser.parser.parse(fileLoc);
                System.out.println(prog.toJSON());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
