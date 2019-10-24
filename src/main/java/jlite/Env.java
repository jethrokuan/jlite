package jlite;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
    private final Multimap<String, Ast.Typ> store = ArrayListMultimap.create();

    Env() {
        this.parent = null;
    }

    Env(Env parent) {
        this.parent = parent;

    }

    /**
     * Populates the environment with type details from a class descriptor.
     *
     * @param desc Clas Descriptor
     */
    void populate(ClasDescriptor desc) {
//        Populate methods first
        for (Map.Entry<String, Set<Ast.FuncTyp>> entry : desc.methods.entrySet()) {
            String method = entry.getKey();
            for (Ast.FuncTyp t : entry.getValue()) {
                put(method, t);
            }
        }

        for (Map.Entry<String, Ast.VarDecl> entry : desc.vars.entrySet()) {
            Ast.Typ t = entry.getValue().type;
            put(entry.getKey(), t);
        }

        // Add "this"
        put("this", new Ast.ClasTyp(desc.cname));
    }

    private void put(String name, Ast.Typ typ) {
        store.put(name, typ);
    }

    Ast.Typ getOne(String name) throws SemanticException {
        Collection<Ast.Typ> typList = store.get(name);
        if (typList.isEmpty()) {
            if (parent != null) return parent.getOne(name);
            return null;
        } else {
            if (typList.size() != 1) {
                throw new SemanticException(String.format("Environment contains more than one of '%s': '%s'", name, typList.toString()));
            }
            return typList.iterator().next();
        }
    }

    ArrayList<SemanticException> populate(Ast.MdDecl mdDecl, HashMap<String, ClasDescriptor> classDescs) {
        ArrayList<SemanticException> errors = new ArrayList<>();

        for (Ast.VarDecl varDecl : mdDecl.args) {
            store.put(varDecl.ident, varDecl.type);
        }

        store.put("Ret", mdDecl.retTyp);

        // Override vars
        for (Ast.VarDecl varDecl : mdDecl.vars) {
            if (!isValidType(varDecl.type, classDescs)) {
                errors.add(new SemanticException(varDecl, String.format("invalid variable type '%s'", varDecl.type)));
                continue;
            }
            put(varDecl.ident, varDecl.type);
        }

        return errors;
    }

    private boolean isValidType(Ast.Typ type, HashMap<String, ClasDescriptor> classDescs) {
        return !(type instanceof Ast.ClasTyp) || classDescs.containsKey(((Ast.ClasTyp) type).cname);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String parentString = parent == null ? "{}" : parent.toString();
        sb.append("parent: ").append(parentString).append("\n");
        sb.append("locals: ")
                .append(store.toString());
        return sb.toString();
    }

    boolean contains(String name) {
        return !store.get(name).isEmpty();
    }

    public Collection<Ast.Typ> get(String name) {
        Collection<Ast.Typ> typList = store.get(name);
        if (typList.isEmpty()) {
            if (parent != null) return parent.get(name);
            return null;
        } else {
            return typList;
        }
    }
}
