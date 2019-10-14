package jlite;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jlite.exceptions.SemanticErrors;
import jlite.exceptions.SemanticException;
import jlite.parser.Ast;

import java.util.*;

/**
 * Environment for a Jlite program.
 * The environment maps variables declared locally to their types.
 * Here, we implement lexical scoping; a child environment is able to access items in the parent environment.
 */
public class Env {
    private final Env parent;
    private final HashMap<String, ClasDescriptor> classDescs;
    private final Multimap<String, Ast.Typ> store = ArrayListMultimap.create();

    public Env(HashMap<String, ClasDescriptor> classDescs) {
        this.parent = null;
        this.classDescs = classDescs;
    }

    public Env(Env parent, HashMap<String, ClasDescriptor> classDescs) {
        this.parent = parent;
        this.classDescs = classDescs;

    }

    /**
     * Populates the environment with type details from a class descriptor.
     *
     * @param desc Clas Descriptor
     */
    public void populate(ClasDescriptor desc) {
//        Populate methods first
        for (Map.Entry<String, Set<Ast.FuncTyp>> entry : desc.methods.entrySet()) {
            String method = entry.getKey();
            for (Ast.FuncTyp t : entry.getValue()) {
                store.put(method, t);
            }
        }

        for (Map.Entry<String, Ast.VarDecl> entry : desc.vars.entrySet()) {
            String varname = entry.getKey();
            Ast.Typ t = entry.getValue().type;
            store.put(entry.getKey(), t);
        }

        // Add "this"
        store.put("this", new Ast.ClasTyp(desc.cname));
    }

    public void put(String name, Ast.Typ typ) {
        store.put(name, typ);
    }

    public Ast.Typ get(String name) {
        Collection<Ast.Typ> typList = store.get(name);
        if (typList.isEmpty()) {
            if (parent != null) return parent.get(name);
            return null;
        } else {
            return typList.iterator().next();
        }
    }

    public ArrayList<SemanticException> populate(Ast.MdDecl mdDecl) {
        ArrayList<SemanticException> errors = new ArrayList<>();

        for (Ast.VarDecl varDecl : mdDecl.args) {
            store.put(varDecl.ident, varDecl.type);
        }

        store.put("Ret", mdDecl.retTyp);

        // Override vars
        for (Ast.VarDecl varDecl : mdDecl.vars) {
            if (!isValidType(varDecl.type)) {
                errors.add(new SemanticException(String.format("invalid variable type '%s'", varDecl.type)));
                continue;
            }
            store.put(varDecl.ident, varDecl.type);
        }

        return errors;
    }

    private boolean isValidType(Ast.Typ type) {
        if (type instanceof Ast.ClasTyp) return classDescs.containsKey(((Ast.ClasTyp) type).cname);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String parentString = parent == null ? "{}" : parent.toString();
        sb.append("parent: " + parentString).append("\n");
        sb.append("locals: ")
                .append(store.toString());
        return sb.toString();
    }

    public boolean contains(String name) {
        return this.get(name) != null;
    }
}
