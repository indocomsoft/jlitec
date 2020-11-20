class Main {
    Void main() {
        Bool a;
        Bool b;
        Bool c;
        Bool d;
        X x;
        x = new X();
        a = true;
        b = true;
        c = x.x();
        d = x.x();
        if (a || b) {
            println("1");
        } else { a = a; }

        if (a && b || c && d) {
            println("2");
        } else { a = a; }

        if ((a || b) && c && d) {
            println("3");
        } else { a = a; }

        while (a && b && c && d) {
            c = false;
            println("4");
        }

        x.getReady();
        if (x.x.x.x.y || a || x.y(true) || x.a().x.a().y && b && c) {
            println("5");
        } else { a = a; }
    }
}

class X {
    X x;
    Bool y;
    Void getReady() {
      y = true;
      x = new X();
      x.y = true;
      x.x = new X();
      x.x.y = true;
      x.x.x = new X();
      x.x.x.y = true;
      x.x.x.x = new X();
      x.x.x.x.y = true;
    }
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
