package jlite.exceptions;

import jlite.parser.Ast;

public class SemanticException extends Exception {
    public SemanticException(String msg) {
        super(msg);
    }

    public SemanticException(Ast.Locatable sym, String msg) {
        super(String.format("%s:%s: %s", sym.location.line, sym.location.col, msg));
    }
}
