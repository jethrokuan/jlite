package jlite.exceptions;

import java.util.ArrayList;

public class SemanticErrors extends Throwable {
    ArrayList<SemanticException> errors;
    public SemanticErrors(ArrayList<SemanticException> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (SemanticException e : errors) {
            s.append(e.toString()).append("\n");
        }

        return s.toString();
    }
}
