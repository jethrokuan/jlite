package jlite.lexer;

public class LexException extends RuntimeException {
    public LexException(String message, int line, int column) {
        super("[" + (line + 1) + ":" + (column + 1) + "] " + message);
    }
}
