package jlite;

import jlite.parser.Ast;

import java.util.HashMap;

public class ClasDescriptor {
    public final Ast.Clas clas;
    public HashMap<String, Ast.VarDecl> vars = new HashMap<>();
    public HashMap<String, String> methods =new HashMap<>();

    public ClasDescriptor(Ast.Clas clas) {
        this.clas = clas;
    }
}
