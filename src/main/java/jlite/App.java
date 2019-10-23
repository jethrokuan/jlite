/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package jlite;

import jlite.exceptions.SemanticException;
import jlite.parser.Ast;
import jlite.parser.parser;

public class App {
    public static void main(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String fileLoc = argv[i];
            try {
                Ast.Prog prog = parser.parse(fileLoc);
                StaticChecker checker = new StaticChecker();
                checker.run(prog);
                System.out.println(prog.toJSON());
            }
            catch (SemanticException e) {
                System.out.println(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
