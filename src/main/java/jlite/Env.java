package jlite;

import jlite.parser.Ast;

/**
 * Environment for a Jlite program.
 * The environment maps variables declared locally to their types.
 * Here, we implement lexical scoping; a child environment is able to access items in the parent environment.
 */
public class Env {
    private final Env parent;

    public Env() {
        this.parent = null;
    }

    public Env(Env parent) {
        this.parent = parent;
    }
}
