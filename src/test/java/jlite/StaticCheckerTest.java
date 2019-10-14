package jlite;

import jlite.exceptions.SemanticErrors;
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
        } catch (SemanticErrors semanticErrors) {
            assert(semanticErrors.toString().contains("Duplicate class name 'Dummy'"));
        }
    }

    @Test
    public void testDuplicateVars() {
        Ast.Prog prog = null;
        try {
            prog = parser.parse("./test/staticchecker/duplicate_vars.j");
        } catch (Exception e) {
            e.printStackTrace();
        }
        StaticChecker checker = new StaticChecker();

        try {
            checker.run(prog);
            assert(false);
        } catch (SemanticErrors semanticErrors) {
            assert(semanticErrors.toString().contains("Duplicate var declaration 'i'"));
        }
    }

    @Test
    public void testDuplicateArgs() {
        Ast.Prog prog = null;
        try {
            prog = parser.parse("./test/staticchecker/duplicate_args.j");
        } catch (Exception e) {
            e.printStackTrace();
        }
        StaticChecker checker = new StaticChecker();

        try {
            checker.run(prog);
            assert(false);
        } catch (SemanticErrors semanticErrors) {
            assert(semanticErrors.toString().contains("Duplicate arg name 'a' in method 'main' of class 'Main'"));
        }
    }

    @Test
    public void testInvalidType() {
        Ast.Prog prog = null;
        try {
            prog = parser.parse("./test/staticchecker/invalid_type.j");
        } catch (Exception e) {
            e.printStackTrace();
        }
        StaticChecker checker = new StaticChecker();

        try {
            checker.run(prog);
            assert(false);
        } catch (SemanticErrors semanticErrors) {
            assert(semanticErrors.toString().contains("Invalid var type 'NonExistent'\n"));
        }
    }
}
