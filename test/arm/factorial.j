class Main {
    Void main () {
        Factorial f;
        Int x;

        f = new Factorial();
        x = f.fact(10);
        println(x);
    }
}

class Factorial {
    Int fact (Int n) {
        Factorial f;

        if (n == 0) {
            return 1;
        } else {
            f = new Factorial();
            return n * f.fact(n - 1);
        }
    }
}
