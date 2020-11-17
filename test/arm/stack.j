class A {
  Void main() {
    new B().f();
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
}
