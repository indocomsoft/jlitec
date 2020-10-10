class A{
  Void main() {
    return;
  }
}

class B {
  Void f() {
    f(new A());
    return;
  }
  Void f(Int a) { return; }
  Void f(String a) { return; }
  Void f(Bool a) { return; }
}
