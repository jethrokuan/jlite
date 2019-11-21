class Main {
    Void main() {
        Dummy d;
        Int x;
        d = new Dummy();
        x = d.add(1,2,3,4,5);
        println(x);
        return;
    }
}

class Dummy {
    Int a;
    Int b;
    Int add(Int a, Int b, Int c, Int d, Int e) {
        return a + b + c + d + e;
    }
}