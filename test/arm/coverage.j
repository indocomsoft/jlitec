class Main {
  Void main() {
    A a;
    Bool b;
    Int c;
    Int d;
    Int e;
    Int f;
    Int g;
    Int h;
    Int i;
    Int j;
    Int k;
    Int l;
    Int m;
    Int n;
    Int o;
    Int p;
    Bool q;
    String s;
    String t;
    A u;
    b = true;
    u = new A();
    u.field = 2 * 120;
    println(u.getField());
    println("Hello" + " world" + "!");
    println("What is your name?");
    readln(s);
    a.s = "Your name is" + s;
    println(a.s);
    a.s = s + " boom";
    u.s = "Hello" + " world";
    readln(q);
    a = new A();
    b = a.f();
    c = a.rand();
    readln(d);
    e = a.rand();
    f = a.rand();
    g = a.rand();
    h = a.rand();
    i = -a.rand();
    k = a.rand();
    l = a.rand();
    m = a.rand();
    n = a.rand();
    o = a.rand();
    p = a.rand();
    j = c + d;
    a.setField(5);
    c = c + g;
    d = d + h;
    e = e * i;
    f = f + j;
    g = g + k;
    h = h + l;
    i = i + m;
    j = j - n;
    u.setField(k + o * 8);
    l = l + p / u.rand();
    n = n + (o - m);
    u.setField(e);
    println(!b);
    a.b = !b;
    u.b = !true;
    u.s = "oof";
    u.field = -c;
    u.field = a.field;
    u.field = a.rand();
    println(s);
    println(b);
    println(c);
    println(d <= c);
    if (e >= 100) {
      println("Wow!");
    } else {
      println("boo");
    }
    println(e >= 100);
    println(f < 5);
    println(g > 5);
    println(h == 5);
    println(i != 0);
    println(j);
    println(k);
    println(l);
    println(m);
    println(n);
    println(o);
    println(p);
    println(s);
    println(q);
    println(true);
    println(null);
    println(a.field);
    println(u.field);
  }
}

class A {
  Int field;
  String s;
  Bool b;

  Void setField(Int field) {
    this.field = field * 2;
  }

  Int getField() {
    return this.field;
  }

  Bool f() {
    return true && false;
  }

  Bool f(Bool a, Bool b, Bool c, Bool d) {
    return a || b || c || d;
  }

  Int rand() {
    Int a;
    Int b;
    Int c;
    a = 30;
    b = 9 - (a / 5);

    c = b * 4;
    if (c > 10) {
      c = c - 10;
    } else {
      c = c;
    }
    return c * (60 / a);
  }


  Void spill(Int c, Int d, Int e, A u) {
    A a;
    Bool b;
    Int f;
    Int g;
    Int h;
    Int i;
    Int j;
    Int k;
    Int l;
    Int m;
    Int n;
    Int o;
    Int p;
    Bool q;
    String s;
    String t;
    b = f(true, false, true, false);
    u = new A();
    u.field = -120;
    println(u.getField());
    println("Hello" + " world" + "!");
    println("What is your name?");
    readln(s);
    a.s = s + s;
    u.field = u.field * 3;
    println("Your name is " + s + null);
    readln(q);
    a = new A();
    b = a.f();
    f = -5;
    g = a.rand();
    h = a.rand();
    i = -a.rand();
    k = a.rand();
    l = a.rand();
    m = a.rand();
    n = a.rand();
    q = !q;
    b = !b || true;
    o = a.rand();
    p = a.rand();
    j = c + d;
    a.setField(5);
    c = c + g;
    d = d + h;
    e = e * i;
    f = f + j;
    g = g + k;
    h = h + l;
    q = !q;
    i = i + m;
    j = j - n;
    u.setField(2 * k + o * 8);
    l = l + p / u.rand();
    n = n + (o - m);
    println(!b);
    q = !q && q;
    println(s);
    println(b);
    println(c);
    println(d <= c);
    if (f >= 100) {
      println("Wow!");
    } else {
      println("boo");
    }
    q = !q;
    println(e >= 100);
    println(f < 5);
    if (!q) {
      println(g > 5);
    } else {
      println(h == 5);
    }
    println(i != 0);
    if (100 < j) {
      println(j);
    } else {
      println(k);
    }
    println(l);
    if (1 == 1) {
      println(m);
    } else {
      println(n);
    }
    println(o);
    q = !true && q;
    println(p);
    println(s);
    println(q);
    println(null);
    println(true);
    println(a.field);
    println(u.field);
  }
}
