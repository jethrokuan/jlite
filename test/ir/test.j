class MainC {
    Void main (){
        Functional fo ;
        Int i;
        Int j ;
        i = 1 + 2 + 3;
        readln(i) ;
        while(j>0) {
            println(j);
        }
        if (i > 0) {
            fo = new Functional() ;
            println(j) ;
        }
        else {
            println("Error") ;
        }
        return ;
    }
}

class Dummy {
  Int a;
  Int b;

  Int test (Int a) {
      return 4;
  }
}

class Functional {
    Int a;
    Dummy d;
    Int i;
    Int f (Int b){
        readln(a);
        a = 4;
        d.a = 4;
        i = d.a;
        if (! (1 == 2)) {
            println("false");
        } else {
            println("true");
        }
        d.test(2);
        return 3;
    }
}