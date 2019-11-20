package jlite.pass;

import jlite.ir.Ir3;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class PassUtils {
    public static void write(String filename, Ir3.Prog prog) {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println(prog.print());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
