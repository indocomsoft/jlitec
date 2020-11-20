class A {
  Void main() {
    new B().f();
    new B().h(1, 2, 3, 4, 5, 6, "hi", null);
  }
}

class B {
  Void f() {
    println(g(1, 2, 3, 4, 5, 6));
  }
  Int g(Int a, Int b, Int c, Int d, Int e, Int f) {
    Int result;
    result = a + b + c + d + e + f;
    println(result);
    return result;
  }
  Void h(Int a, Int b, Int c, Int d, Int e, Int f, String g, String h) {
    println(g + h);
    println(g(a, b, c, d, e, f));
  }
}
