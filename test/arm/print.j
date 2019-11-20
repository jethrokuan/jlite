class Main {
    Void main() {
        Printer p;
        p.printInt(1);
        p.print("Hello World");
    }
}

class Printer {
    Void printInt(Int a) {
        println(a);
        return;
    }

    Void print(String s) {
        println(s);
        return;
    }
}