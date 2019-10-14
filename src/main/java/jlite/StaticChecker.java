package jlite;

import jlite.exceptions.SemanticErrors;
import jlite.exceptions.SemanticException;
import jlite.parser.Ast;

import java.util.ArrayList;
import java.util.HashMap;

public class StaticChecker {
    private HashMap<String, ClasDescriptor> clasDescriptors = new HashMap<>();
    public void run(Ast.Prog prog) throws SemanticErrors {
        this.init(prog);
    }

    /**
     * Initializes the Static Checker, by populating the hashmap of class descriptors.
     *
     * While populating, check the following:
     * 1. There are no 2 classes with the same name
     * 2. For each class, no 2 fields have the same name
     * 3. The types of every varDecl is valid
     *
     *
     * @param prog AST generated by the parser
     * @throws SemanticErrors
     */
    private void init(Ast.Prog prog) throws SemanticErrors {
        ArrayList<SemanticException> errors = new ArrayList<>();

        for (Ast.Clas c: prog.clasList) {
            // Check if class name already exists
            if (clasDescriptors.containsKey(c.cname)) {
                errors.add(new SemanticException(String.format("Duplicate class name '%s'", c.cname)));
            }

            ClasDescriptor desc = new ClasDescriptor(c);
            clasDescriptors.put(c.cname, desc);
        }

        if (!errors.isEmpty()) {
            throw new SemanticErrors(errors);
        }

        for (Ast.Clas c : prog.clasList) {
            ClasDescriptor desc = clasDescriptors.get(c.cname);

            for (Ast.VarDecl varDecl : c.varDeclList) {
                // Check if the declaration types are valid
                if (!isValidType(varDecl.type)) {
                    errors.add(new SemanticException(String.format("Invalid var type '%s'", varDecl.type.cname)));
                }

                // Check if there are duplicate var declarations
                if (desc.vars.containsKey(varDecl.ident)) {
                    errors.add(new SemanticException(String.format("Duplicate var declaration '%s'", varDecl.ident)));
                } else {
                    desc.vars.put(varDecl.ident, varDecl);
                }
            }

            for (Ast.MdDecl mdDecl : c.mdDeclList) {
                HashMap<String, Ast.VarDecl> args = new HashMap<>();

                for (Ast.VarDecl arg : mdDecl.args) {
                    if (args.containsKey(arg.ident)) {
                        errors.add(new SemanticException(String.format("Duplicate arg name '%s' in method '%s' of class '%s'", arg.ident, mdDecl.name, c.cname)));
                    }

                    if (!isValidType(arg.type)) {
                        errors.add(new SemanticException(String.format("Invalid argument type '%s' for arg '%s' in method '%s' of class '%s'", arg.type, arg.ident, mdDecl.name, c.cname)));
                    }

                    args.put(arg.ident, arg);
                }
            }
        }

        if (!errors.isEmpty()) throw new SemanticErrors(errors);
    }

    /**
     * @param type
     * @return True if a base type (String etc.) or it is a class type and the program has a class with that name
     */
    private boolean isValidType(Ast.Typ type) {
        if (type.typ == Ast.JliteTyp.CLASS) return clasDescriptors.containsKey(type.cname);
        return true;
    }
}
