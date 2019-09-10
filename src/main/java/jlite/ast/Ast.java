package jlite.ast;

import java.util.*;

public class Ast {
    public static class Prog {
        public final List<Clas> clasList;

        public Prog(List<Clas> clasList) {
            this.clasList = Collections.unmodifiableList(new ArrayList<>(clasList));
        }
    }

    public static class Clas {
        public final String className;

        public Clas(String className) {
            this.className = className;
        }
    }
}
