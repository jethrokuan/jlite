package jlite;

import jlite.parser.Ast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ClasDescriptor {
    public final String cname;
    public HashMap<String, Ast.VarDecl> vars = new HashMap<>();
    public HashMap<String, Set<Ast.FuncTyp>> methods = new HashMap<>();

    public ClasDescriptor(Ast.Clas clas) {
        this.cname = clas.cname;
    }

    public boolean hasMethodSignature(String method, Ast.FuncTyp funcTyp) {
        return methods.containsKey(method) && methods.get(method).contains(funcTyp);
    }

    public void addMethodSignature(String method, Ast.FuncTyp funcTyp) {
        assert (!hasMethodSignature(method, funcTyp));
        if (methods.containsKey(method)) {
            methods.get(method).add(funcTyp);
        } else {
            HashSet<Ast.FuncTyp> s = new HashSet<>();
            s.add(funcTyp);
            methods.put(method, s);
        }
    }
}
