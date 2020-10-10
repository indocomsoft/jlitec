class A {
  Void main() {
    new B().f(1, "2");
    return;
  }
}

class B {
  B baa;
  Int z;
  Bool b;
  Void f(Int a, String b) {
    Bool c;
    z = 5;
    this.baa = this;
    this.b = false;
    c = this.b;
    while (a < 10) {
      a = a + 1;
    }
    while (a > 1) {
      a = a - 1;
    }
    while (a <= 10) {
      a = a + 1;
    }
    while (a >= 1) {
      a = a - 1;
    }
    while (a != 10) {
      a = a + 1;
    }
    while (a == 0) {
      a = a + 1;
    }
    while (a == 1 || a == 0) {
      a = a + 1;
    }
    while (this.b) {
      a = a * 2;
    }
    while (c) {
      a = a * 2;
    }
    if (c) {
      a = a * 2;
    } else {
      a = z;
    }
    if (true || false) {
      a = 5;
    } else {
      a = 6;
    }
    if (true) {
      a = 5;
    } else {
      a = 6;
    }
    if (!false) {
      a = 5;
    } else {
      a = 6;
    }
    this.baa.baa.asd(null);
    new B().baa.baa.asd(null);
    g().baa.baa.asd(null);
    return;
  }

  Void asd(A a) {
    return;
  }

  B g() {
    return new B();
  }

  Int a(Int a) {
    while (true) {
      if (a == 10) {
        return a;
      } else {
        return a + 1;
      }
    }
  }
}
