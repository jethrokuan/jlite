package jlite;

import jlite.exceptions.SemanticException;
import jlite.parser.Ast;
import jlite.parser.parser;
import org.junit.Test;

public class StaticCheckerTest {
    @Test
    public void testDuplicateClass() {
        Ast.Prog prog = null;
        try {
            prog = parser.parse("./test/staticchecker/duplicate_class.j");
        } catch (Exception e) {
            e.printStackTrace();
        }
        StaticChecker checker = new StaticChecker();

        try {
            checker.run(prog);
            assert(false);
        } catch (SemanticException e) {
            assert(e.toString().contains("Duplicate class name 'Dummy'"));
        }
    }
}
