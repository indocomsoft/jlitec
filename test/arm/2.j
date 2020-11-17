class Main {
    Void main() {
        Bool a;
        Bool b;
        Bool c;
        Bool d;
        X x;
        if (a || b) {
            println("1");
        } else { a = a; }

        if (a && b || c && d) {
            println("2");
        } else { a = a; }

        if ((a || b) && c && d) {
            println("3");
        } else { a = a; }

        while (a || b || c || d) {
            println("4");
        }

        if (new X().x.x.x.y || a || x.y(true) || x.a().x.a().y && b && c) {
            println("5");
        } else { a = a; }
    }
}

class X {
    X x;
    Bool y;
    Bool x() {
        return true;
    }
    Bool y(Bool y) {
        return new X().y(y);
    }
    X a() {
        return a().a().a();
    }
}
