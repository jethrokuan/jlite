class MainC {
    Void main (){
        Functional fo ;
        Int i;
        Int j ;
        readln(i) ;
        j = 3 + 5 + 7;
        if (i > 0) {
            fo = new Functional() ;
            j = fo.f(i) ;
            println(j) ;
        }
        else {
            println("Error") ;
        }
        return ;
    }
}

class Functional {
    Int a;
    Int f (Int b){
        return 3;
    }
}